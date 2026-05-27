using Newtonsoft.Json;

namespace Core.Libs.Loto
{
    public class ReturnObject
    {
        [JsonProperty(Order = 1)] public string Status { get; set; }

        [JsonProperty(Order = 2)] public string Message { get; set; }

        [JsonProperty(Order = 3)] public string Data { get; set; }

        [JsonProperty(Order = 4)] public object DataObj { get; set; }

        public string ToJson() =>
            JsonHelper.SerializeObject(this, JsonHelper.CONVERT_SETTINGS);

        public static ReturnObject FromJson(string json) =>
            JsonHelper.DeserializeObject<ReturnObject>(json, JsonHelper.CONVERT_SETTINGS);
    }

    public class ReturnDataObject : ReturnObject
    {
        [JsonProperty(Order = 4)] public new object Data { get; set; }
    }
}