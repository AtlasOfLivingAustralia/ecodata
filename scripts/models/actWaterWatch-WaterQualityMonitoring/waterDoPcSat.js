// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/2318f116-ff07-4609-8d04-e978b640d0e9
// Activity Name: ACT Water Watch - Water Quality Monitoring.
// Output model name: actWaterwatch_waterQualitySurvey
// jsMain: https://dl.dropbox.com/s/55ytuve6qjdi62o/waterDissolvedOxygenInPercentSaturation.js?dl=0
self.data.waterDissolvedOxygenInPercentSaturation = ko.observable();
self.data.waterDissolvedOxygenInMilligramsPerLitre.subscribe(function(disolvedOxygen) {
    var waterTemp = self.data.waterTemperatureInDegreesCelcius();
    self.transients.calculateSaturation(waterTemp, disolvedOxygen);
});
self.data.waterTemperatureInDegreesCelcius.subscribe(function(waterTemp) {
    var disolvedOxygen = self.data.waterDissolvedOxygenInMilligramsPerLitre();
    self.transients.calculateSaturation(waterTemp, disolvedOxygen);
});

self.transients.calculateSaturation = function(waterTemp, disolvedOxygen){
    var saturationFactor = [
        14.62,  // 0C
        14.22,
        13.83,
        13.46,
        13.11,
        12.77,
        12.45,
        12.14,
        11.84,
        11.56,
        11.29,  // 10C
        11.03,
        10.78,
        10.54,
        10.31,
        10.08,
        9.87,
        9.67,
        9.47,
        9.28,
        9.09,   // 20C
        8.92,
        8.72,
        8.58,
        8.42,
        8.26,
        8.11,
        7.97,
        7.83,
        7.69,
        7.56,   // 30C
        7.43,
        7.31,
        7.18,
        7.07,
        6.95,
        6.84,
        6.73,
        6.62,
        6.52,
        6.41,   // 40C
        6.31,
        6.21,
        6.12,
        6.02,
        5.93    // 45C
        ] ;     
 
        var saturation = 0;
         
        if ( isNaN(waterTemp) || isNaN(disolvedOxygen) )
        {
            saturation = ''; 
        }
        else if ( waterTemp < 0 || waterTemp >= saturationFactor.length )
        {
            blockUIWithMessage("<p class='text-center'>ERROR</p><p class='text-center'>Water temperature cannot be more that 45C</p>");
            setTimeout(function(){
                $.unblockUI();
            }, 2000);
            saturation = '';
            self.data.waterTemperatureInDegreesCelcius(0);
        }
        else
        {
            saturation = Math.round(disolvedOxygen / saturationFactor[waterTemp] * 100);
        }
        self.data.waterDissolvedOxygenInPercentSaturation(saturation);
};