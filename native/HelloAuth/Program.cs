using System;
using System.Threading.Tasks;
using Windows.Security.Credentials.UI;

class Program
{
    static async Task<int> Main(string[] args)
    {
        string msg = args.Length > 0 ? args[0] : "NEXORA Bank admin verification";

        var availability = await UserConsentVerifier.CheckAvailabilityAsync();
        if (availability != UserConsentVerifierAvailability.Available)
        {
            Console.Error.WriteLine("NOT_AVAILABLE: " + availability);
            return 2;
        }

        var result = await UserConsentVerifier.RequestVerificationAsync(msg);

        if (result == UserConsentVerificationResult.Verified)
            return 0;

        Console.Error.WriteLine("FAILED: " + result);
        return 1;
    }
}
