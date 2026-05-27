import json
import unittest
from pathlib import Path
import sys
import tempfile

sys.path.insert(0, str(Path(__file__).parent))
import gen_openapi  # noqa: E402


class TestModuleLoads(unittest.TestCase):
    def test_module_has_main(self):
        self.assertTrue(callable(gen_openapi.main))


class TestParseCommands(unittest.TestCase):
    def test_extracts_id_name_path(self):
        xml = """<?xml version="1.0"?>
<backend><commands>
  <command>
    <id>100</id>
    <name>update money user</name>
    <path>com.vinplay.api.backend.processors.money.UpdateMoneyUserProcessor</path>
  </command>
  <command>
    <id>102</id>
    <name>get user by nickname</name>
    <path>com.vinplay.api.backend.processors.GetUserByNickNameProcessor</path>
  </command>
</commands></backend>"""
        cmds = gen_openapi.parse_commands(xml)
        self.assertEqual(len(cmds), 2)
        self.assertEqual(cmds[0], (100, "update money user",
                                   "com.vinplay.api.backend.processors.money.UpdateMoneyUserProcessor"))
        self.assertEqual(cmds[1][0], 102)

    def test_skips_xml_comments(self):
        xml = """<commands>
  <!--
  <command><id>999</id><name>old</name><path>x.Y</path></command>
  -->
  <command><id>100</id><name>real</name><path>x.Z</path></command>
</commands>"""
        cmds = gen_openapi.parse_commands(xml)
        self.assertEqual([c[0] for c in cmds], [100])

    def test_warns_on_duplicate_ids(self):
        xml = """<commands>
  <command><id>100</id><name>a</name><path>x.A</path></command>
  <command><id>100</id><name>b</name><path>x.B</path></command>
</commands>"""
        cmds = gen_openapi.parse_commands(xml)
        # First occurrence wins; duplicate is dropped.
        self.assertEqual(len(cmds), 1)
        self.assertEqual(cmds[0][2], "x.A")


class TestResolveSourcePath(unittest.TestCase):
    def test_finds_existing_file_in_first_root(self):
        with tempfile.TemporaryDirectory() as root:
            target = Path(root) / "com" / "vinplay" / "Foo.java"
            target.parent.mkdir(parents=True)
            target.write_text("class Foo {}")
            result = gen_openapi.resolve_source_path(
                "com.vinplay.Foo", [Path(root)]
            )
            self.assertEqual(result, target)

    def test_returns_none_when_missing(self):
        with tempfile.TemporaryDirectory() as root:
            result = gen_openapi.resolve_source_path(
                "com.vinplay.Missing", [Path(root)]
            )
            self.assertIsNone(result)

    def test_searches_multiple_roots(self):
        with tempfile.TemporaryDirectory() as r1, tempfile.TemporaryDirectory() as r2:
            target = Path(r2) / "x" / "Y.java"
            target.parent.mkdir(parents=True)
            target.write_text("class Y {}")
            result = gen_openapi.resolve_source_path("x.Y", [Path(r1), Path(r2)])
            self.assertEqual(result, target)


class TestExtractParams(unittest.TestCase):
    def test_finds_basic_calls(self):
        src = """
        String un = request.getParameter("un");
        String pw = request.getParameter("pw");
        """
        self.assertEqual(gen_openapi.extract_params(src), ["un", "pw"])

    def test_dedupes_preserving_first_seen_order(self):
        src = """
        request.getParameter("un");
        request.getParameter("pw");
        request.getParameter("un");
        """
        self.assertEqual(gen_openapi.extract_params(src), ["un", "pw"])

    def test_tolerates_whitespace_inside_call(self):
        src = 'request.getParameter(  "abc"  );'
        self.assertEqual(gen_openapi.extract_params(src), ["abc"])

    def test_returns_empty_for_no_calls(self):
        self.assertEqual(gen_openapi.extract_params("class Foo {}"), [])


class TestDeriveTag(unittest.TestCase):
    def test_uses_last_subpackage(self):
        self.assertEqual(
            gen_openapi.derive_tag("com.vinplay.api.backend.processors.money.UpdateProcessor"),
            "money",
        )
        self.assertEqual(
            gen_openapi.derive_tag("com.vinplay.api.backend.processors.report.GetCCUFilteredProcessor"),
            "report",
        )

    def test_root_processor_returns_general(self):
        self.assertEqual(
            gen_openapi.derive_tag("com.vinplay.api.backend.processors.GetUserByNickNameProcessor"),
            "general",
        )
        self.assertEqual(
            gen_openapi.derive_tag("com.vinplay.api.processors.LoginProcessor"),
            "general",
        )


class TestBuildOperation(unittest.TestCase):
    def test_backend_command_inherits_global_aat_security(self):
        op = gen_openapi.build_operation(
            api="backend",
            cmd_id=100,
            name="update money user",
            fqn="com.vinplay.api.backend.processors.money.UpdateMoneyUserProcessor",
            params=["nn", "am"],
        )
        self.assertEqual(op["tags"], ["money"])
        self.assertEqual(op["summary"], "update money user")
        self.assertEqual(op["operationId"], "backend_c100_updateMoneyUser")
        # aat is no longer a per-op parameter — it is the global security scheme.
        param_names = [p["name"] for p in op["parameters"]]
        self.assertEqual(param_names, ["c", "nn", "am"])
        # No security override → inherits the doc-level [{"AatAuth": []}].
        self.assertNotIn("security", op)
        # `c` enum constrained to this id.
        c_param = op["parameters"][0]
        self.assertEqual(c_param["schema"]["enum"], [100])

    def test_backend_login_command_overrides_security_to_empty(self):
        op = gen_openapi.build_operation(
            api="backend", cmd_id=701, name="login admin",
            fqn="com.vinplay.api.backend.processors.login.AdminLoginProcessor",
            params=["un", "pw"],
        )
        param_names = [p["name"] for p in op["parameters"]]
        self.assertEqual(param_names, ["c", "un", "pw"])
        # Login issues the aat, so it must not require one.
        self.assertEqual(op["security"], [])

    def test_portal_command_overrides_security_to_empty(self):
        op = gen_openapi.build_operation(
            api="portal", cmd_id=3, name="login",
            fqn="com.vinplay.api.processors.LoginProcessor",
            params=["un", "pw"],
        )
        self.assertEqual(op["tags"], ["general"])
        self.assertEqual(op["operationId"], "portal_c3_login")
        param_names = [p["name"] for p in op["parameters"]]
        self.assertEqual(param_names, ["c", "un", "pw"])
        # Portal commands authenticate via session/access-token, never aat.
        self.assertEqual(op["security"], [])

    def test_response_is_envelope_ref(self):
        op = gen_openapi.build_operation(
            api="portal", cmd_id=9, name="get time server",
            fqn="com.vinplay.api.processors.GetTimeServerProccessor", params=[],
        )
        ref = op["responses"]["200"]["content"]["application/json"]["schema"]["$ref"]
        self.assertEqual(ref, "#/components/schemas/Envelope")


class TestBuildDoc(unittest.TestCase):
    def test_doc_has_required_top_level_keys(self):
        doc = gen_openapi.build_doc(backend_ops={}, portal_ops={})
        self.assertEqual(doc["openapi"], "3.0.3")
        self.assertIn("info", doc)
        self.assertIn("servers", doc)
        self.assertIn("components", doc)
        self.assertIn("paths", doc)

    def test_components_define_envelope_and_aat_security_scheme(self):
        doc = gen_openapi.build_doc(backend_ops={}, portal_ops={})
        self.assertIn("Envelope", doc["components"]["schemas"])
        self.assertIn("AatAuth", doc["components"]["securitySchemes"])
        scheme = doc["components"]["securitySchemes"]["AatAuth"]
        self.assertEqual(scheme["type"], "apiKey")
        self.assertEqual(scheme["in"], "query")
        self.assertEqual(scheme["name"], "aat")
        self.assertEqual(doc["security"], [{"AatAuth": []}])

    def test_paths_use_synthetic_template(self):
        op = {"summary": "x", "operationId": "y", "tags": ["t"],
              "parameters": [], "responses": {}}
        doc = gen_openapi.build_doc(
            backend_ops={100: op}, portal_ops={3: op},
        )
        self.assertIn("/api_backend/c/100", doc["paths"])
        self.assertIn("/api/c/3", doc["paths"])
        self.assertEqual(doc["paths"]["/api_backend/c/100"]["post"], op)

    def test_paths_sorted_for_stable_diffs(self):
        doc = gen_openapi.build_doc(
            backend_ops={9999: {}, 100: {}, 200: {}},
            portal_ops={},
        )
        keys = list(doc["paths"].keys())
        self.assertEqual(keys, ["/api_backend/c/100", "/api_backend/c/200",
                                "/api_backend/c/9999"])


class TestEndToEnd(unittest.TestCase):
    def test_main_writes_valid_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            backend_xml = tmp / "api_backend.xml"
            portal_xml = tmp / "api_portal.xml"
            backend_xml.write_text(
                "<commands><command><id>100</id><name>test cmd</name>"
                "<path>com.vinplay.api.backend.processors.foo.TestProcessor</path>"
                "</command></commands>"
            )
            portal_xml.write_text(
                "<commands><command><id>3</id><name>login</name>"
                "<path>com.vinplay.api.processors.LoginProcessor</path>"
                "</command></commands>"
            )

            backend_root = tmp / "backend_src"
            portal_root = tmp / "portal_src"
            (backend_root / "com/vinplay/api/backend/processors/foo").mkdir(parents=True)
            (backend_root / "com/vinplay/api/backend/processors/foo/TestProcessor.java").write_text(
                'request.getParameter("foo");'
            )
            (portal_root / "com/vinplay/api/processors").mkdir(parents=True)
            (portal_root / "com/vinplay/api/processors/LoginProcessor.java").write_text(
                'request.getParameter("un"); request.getParameter("pw");'
            )

            out_path = tmp / "openapi.json"
            rc = gen_openapi.main([
                "--backend-xml", str(backend_xml),
                "--portal-xml", str(portal_xml),
                "--backend-src", str(backend_root),
                "--portal-src", str(portal_root),
                "--output", str(out_path),
            ])
            self.assertEqual(rc, 0)
            self.assertTrue(out_path.exists())
            doc = json.loads(out_path.read_text())
            self.assertEqual(doc["openapi"], "3.0.3")
            self.assertIn("/api_backend/c/100", doc["paths"])
            self.assertIn("/api/c/3", doc["paths"])


if __name__ == "__main__":
    unittest.main()
