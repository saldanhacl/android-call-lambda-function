const pucLocations = [
    {
        name: "Praça da Liberdade",
        lat: -19.932790,
        long: -43.936090
    },
    {
        name: "Coração Eucarístico",
        lat: -19.924424,
        long: -43.991467
    },
    {
        name: "São Gabriel",
        lat: -19.857136,
        long: -43.919364
    },
    {
        name: "Betim",
        lat: -19.954351,
        long: -44.198842
    },
];

const wellcomeMessage = "Bem vindo à PUC Minas unidade "

exports.handler = async (event, context) => {
    for (var i = 0; i < pucLocations.length; i++) {
         var dist = distance(event.lat, event.long, pucLocations[i].lat, pucLocations[i].long)
         if (dist < 100) {
            return wellcomeMessage + pucLocations[i].name
         }
    }
};


function distance(lat1,lon1,lat2,lon2) {
	var R = 6371; // km (change this constant to get miles)
	var dLat = (lat2-lat1) * Math.PI / 180;
	var dLon = (lon2-lon1) * Math.PI / 180;
	var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.cos(lat1 * Math.PI / 180 ) * Math.cos(lat2 * Math.PI / 180 ) *
		Math.sin(dLon/2) * Math.sin(dLon/2);
	var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	var d = R * c;
	if (d>1) return Math.round(d)* 1000;
	else if (d<=1) return Math.round(d*1000);
	return d * 1000;
}
