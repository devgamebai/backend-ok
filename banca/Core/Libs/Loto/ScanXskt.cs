using System;
using System.Linq;
using System.Threading.Tasks;
using Core.Libs.Loto;
using HtmlAgilityPack;
using LotoService;
using SimpleJSON;

namespace Loto
{
    public class ScanXskt
    {
        public async Task<ReturnObject> Run(string ngay = null)
        {
            try
            {
                // ngay = "27/02/2021";
                var currentDate = DateTime.Now;

                if (currentDate.Hour == 18 && currentDate.Minute < 40 || currentDate.Hour < 18 || currentDate.Hour >= 20)
                {
                    return new ReturnObject
                    {
                        Status = Status.STATUS_ERROR,
                        Message = "Kết quả được lấy từ 18h40 -> 20h" +
                                  "(" + currentDate.Hour + ":" + currentDate.Minute + ")"
                    };
                }

                var id = GetIdCurrentDate();
                string target = null;
                if (!string.IsNullOrEmpty(ngay))
                {
                    var dateGet = DateTime.ParseExact(ngay, "dd/MM/yyyy",
                        System.Globalization.CultureInfo.InvariantCulture);
                    id = GetIdByDate(dateGet);
                    target = GetDateXsmbByDateTime(dateGet);
                }
                else
                {
                    target = GetDateXsmbByDateTime(currentDate);
                }

                var ketQua = await LotoSql.GetLotoResult(LotoChannel.MienBac, int.Parse(id));

                if (ketQua != null && !string.IsNullOrEmpty(ketQua.ResultSp))
                {
                    return new ReturnObject
                    {
                        Status = Status.STATUS_SUCCESS,
                        Message = "Kết quả ngày hôm nay đã được đăng"
                    };
                }

                    var getHtmlDoc = new HtmlWeb();
                    HtmlDocument document = getHtmlDoc.Load("https://xskt.com.vn/xsmb/ngay-" + target);
                    var kq = document.GetElementbyId("lastDateStr");

                    if (kq == null)
                    {
                        return new ReturnObject
                        {
                            Status = Status.STATUS_SUCCESS,
                            Message = "Kết quả ngày hôm nay đã được đăng2"
                        };
                    }

                    var checkDate = kq.Attributes.FirstOrDefault(item => item.Value.Equals(target));
                    if (checkDate == null)
                    {
                        return new ReturnObject
                        {
                            Status = Status.STATUS_SUCCESS,
                            Message = "Kết quả ngày hôm nay đã được đăng3"
                        };
                    }

                    Console.WriteLine(kq);


                var data = document.DocumentNode.SelectNodes("//*[@id=\"MB0\"]/tr");

                if (data == null || data.Count != 12)
                {
                    return new ReturnObject
                    {
                        Status = Status.STATUS_SUCCESS,
                        Message = "Kết quả ngày hôm nay đã được đăng4"
                    };
                }

                var db = data[1].InnerText.Substring(2, 5);
                var g1 = data[2].InnerText.Substring(2, 5);
                var g2 = data[3].InnerText.Split(' ');
                var g21 = g2[0].Substring(2, 5);
                var g22 = g2[1].Substring(0, 5);
                var g3 = data[4].InnerText.Split(' ');
                var g31 = g3[0].Substring(2, 5);
                var g32 = g3[1].Substring(0, 5);
                var g33 = g3[2].Substring(0, 5);
                var g34 = g3[2].Substring(5, 5);
                var g35 = g3[3].Substring(0, 5);
                var g36 = g3[4].Substring(0, 5);
                var g4 = data[6].InnerText.Split(' ');
                var g41 = g4[0].Substring(2, 4);
                var g42 = g4[1].Substring(0, 4);
                var g43 = g4[2].Substring(0, 4);
                var g44 = g4[3].Substring(0, 4);
                var g5 = data[7].InnerText.Split(' ');
                var g51 = g5[0].Substring(2, 4);
                var g52 = g5[1].Substring(0, 4);
                var g53 = g5[2].Substring(0, 4);
                var g54 = g5[2].Substring(4, 4);
                var g55 = g5[3].Substring(0, 4);
                var g56 = g5[4].Substring(0, 4);
                var g6 = data[9].InnerText.Split(' ');
                var g61 = g6[0].Substring(2, 3);
                var g62 = g6[1].Substring(0, 3);
                var g63 = g6[2].Substring(0, 3);
                var g7 = data[10].InnerText.Split(' ');
                var g71 = g7[0].Substring(2, 2);
                var g72 = g7[1].Substring(0, 2);
                var g73 = g7[2].Substring(0, 2);
                var g74 = g7[3].Substring(0, 2);
                
                var jsonObject = new JSONObject();
                jsonObject["session"] = DateTime.Now.ToString("yyyyMMdd");
                jsonObject["channel"] = 1;
                var jArr0 = new JSONArray {db};
                var jArr1 = new JSONArray {g1};
                var jArr2 = new JSONArray {g21, g22};
                var jArr3 = new JSONArray {g31, g32, g33, g34, g35, g36};
                var jArr4 = new JSONArray {g41, g42, g43, g44};
                var jArr5 = new JSONArray {g51, g52, g53, g54, g55, g56};
                var jArr6 = new JSONArray {g61, g62, g63};
                var jArr7 = new JSONArray {g71, g72, g73, g74};
                var jArr8 = new JSONArray ();
                var jArrResults = new JSONArray {jArr0, jArr1, jArr2, jArr3, jArr4, jArr5, jArr6, jArr7, jArr8};
                jsonObject["results"] = jArrResults;
                jsonObject["timeResult"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");


                var (item1, item2) = await LotoSql.AddResult(int.Parse(DateTime.Now.ToString("yyyyMMdd")), LotoChannel.MienBac,
                    jArrResults, DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
                Logger.Info("ScanXskt result: " + $"{db}-{g1}-{g21}-{g22}-{g31}-{g32}-{g33}-{g34}-{g35}-{g36}-{g41}" +
                            $"-{g42}-{g43}-{g44}-{g51}-{g52}-{g53}-{g54}-{g55}-{g56}-{g61}-{g62}-{g63}-{g71}-{g72}-{g73}-{g74}");
                return new ReturnObject
                {
                    Status = item1!=0?Status.STATUS_SUCCESS:Status.STATUS_ERROR,
                    Message = item1!=0?"Kết quả ngày hôm nay đã được đăng 5.":"Cập nhật kết quả không thành công."
                };
            }
            catch (Exception e)
            {
                Logger.Error("ScanXskt error: " + e);
            }

            return new ReturnObject()
            {
                Status = Status.STATUS_SUCCESS,
                Message = "Not Found Stake for process"
            };
        }

        private static string GetIdCurrentDate(int next = 0)
        {
            var currentDate = DateTime.Now.AddDays(next);
            return currentDate.Year +
                   (currentDate.Month <= 9 ? "0" + currentDate.Month : currentDate.Month.ToString()) + 
                   (currentDate.Day <= 9 ? "0" + currentDate.Day : currentDate.Day.ToString());
        }

        private static string GetDateXsmbByDateTime(DateTime dateGet)
        {
            return dateGet.Day + "-" + dateGet.Month + "-" + dateGet.Year;
        }

        private static string GetIdByDate(DateTime dateGet)
        {
            return dateGet.Year +
                   (dateGet.Month <= 9 ? "0" + dateGet.Month : dateGet.Month.ToString()) +
                   (dateGet.Day <= 9 ? "0" + dateGet.Day : dateGet.Day.ToString());
        }
    }
}