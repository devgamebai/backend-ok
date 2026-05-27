using System;
using Newtonsoft.Json;

namespace Core.Libs.Loto
{
    public static class JsonHelper
    {
        public static T DeserializeObject<T>(string json, JsonSerializerSettings setting = null)
        {
            return JsonConvert.DeserializeObject<T>(json, setting);
        }

        public static T FromJson<T>(string json)
        {
            return JsonConvert.DeserializeObject<T>(json, CONVERT_SETTINGS);
        }

        public static string ToJson<T>(this T obj)
        {
            return JsonConvert.SerializeObject(obj, CONVERT_SETTINGS);
        }


        public static string SerializeObject(Object obj, JsonSerializerSettings setting = null)
        {
            return JsonConvert.SerializeObject(obj, setting);
        }

        public static readonly JsonSerializerSettings CONVERT_SETTINGS = new JsonSerializerSettings
        {
            MetadataPropertyHandling = MetadataPropertyHandling.Ignore,
            DateParseHandling = DateParseHandling.DateTime,
            NullValueHandling = NullValueHandling.Ignore,
            /*Converters =
            {
                new IsoDateTimeConverter {DateTimeStyles = DateTimeStyles.AssumeUniversal}
            }*/
        };
    }
}