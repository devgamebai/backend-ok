using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Core.Libs.Loto;
using Loto;
using LotoService;

namespace ScanLoto
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                var task1 = Task.Run(RunScan);
                var task2 = Task.Run(RunHandleWinLoss);
                Task.WaitAll(task1,task2);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        private static async Task RunScan()
        {
            Thread.CurrentThread.Name = $"pending:{RandomString(5)}";
            var processCurl = new ScanXskt();
            while (true)
            {
                try
                {
                        var resultSend = await processCurl.Run();
                        Logger.Info($"Result: {JsonHelper.SerializeObject(resultSend)}");
                        Thread.Sleep(300000);
                }
                catch (Exception ex)
                {
                    Logger.Error(ex, $"Result: {ex.Message}");
                    Console.WriteLine(ex.Message);
                }
            }
        }
        private static async Task RunHandleWinLoss()
        {
            Thread.CurrentThread.Name = $"pending:{RandomString(5)}";
            while (true)
            {
                try
                {
                    var result = await LotoSql.CalculateResult();
                    Thread.Sleep(300000);
                }
                catch (Exception ex)
                {
                    Logger.Error(ex, $"Result: {ex.Message}");
                    Console.WriteLine(ex.Message);
                }
            }
        }
        private static string RandomString(int length)
        {
            var random = new Random();
            const string chars = "abcdefghijklmnopqrstuvwxyz0123456789";
            return new string(Enumerable.Repeat(chars, length)
                .Select(s => s[random.Next(s.Length)]).ToArray());
        }
    }
}