function per2RGBArray(p){
	var r,g,b;
	if(0 <= p && p < 0.4){
		// Blue -> Teal -> Green
		r = 0;
		g = Math.ceil(255 * (p/0.4));
		b = Math.ceil(255 - 255 * (p/0.4));
	} else if (p <= 0.6){
		// Green -> Yellow
		r = Math.ceil(255*((p-0.4)/0.2));
		g = 255.0;
		b = 0;
	} else {
		// Yellow -> Red
		r = 255.0;
		g = Math.ceil(255 - 255 * ((p-0.6)/0.4));
		b = 0;
	}
	return [r,g,b];
}

function RGB2Str(array){
	var r,g,b;
	var correctHex = function(i){
		var str = i.toString(16);
		if(str.length == 1){
			str = "0" + str;
		}
		return str;
	}
	r = correctHex(array[0]);
	g = correctHex(array[1]);
	b = correctHex(array[2]);
	return "#" + r + g + b;
}

//var socket = io.connect("http://localhost:3001");
//var socket = io.connect("http://192.168.1.51:3001");
//var serverip = "http://localhost:3001";
var socket = io.connect();
socket.on('news', function (data) {
	//console.log(data);
});


setInterval(function(){
	socket.emit("fetch data", {})
}, 5000);


socket.on("db update", function(data){
			//console.log(data);
			updatemap(data);
		});

function updatemap(dbdata){
	// Is suposed to fetch data from the database

	//console.log(dbdata);
	//socket.emit("dump", dbdata);
	
	svg.selectAll(".country")
		.data(countries)
		.style("fill",function(d, i){

			// d is data from data()
			// i is index
			// Can use d.id to identifiy contries
			if(d.id in dbdata){
				//return RGB2Str(per2RGBArray(dbdata[d.id]));
				return RGB2Str(per2RGBArray(Math.random()));

			} else {
				return "#000000";
			}
		});
}

var width = 960,
		height = 580;

var color = d3.scale.category10();

var projection = d3.geo.kavrayskiy7()
		.scale(170)
		.translate([width / 2, height / 2])
		.precision(.1);

var path = d3.geo.path()
		.projection(projection);

var graticule = d3.geo.graticule();

var svg = d3.select("body").append("svg")
		.attr("width", width)
		.attr("height", height);

svg.append("defs").append("path")
		.datum({type: "Sphere"})
		.attr("id", "sphere")
		.attr("d", path);

svg.append("use")
		.attr("class", "stroke")
		.attr("xlink:href", "#sphere");

svg.append("use")
		.attr("class", "fill")
		.attr("xlink:href", "#sphere");

svg.append("path")
		.datum(graticule)
		.attr("class", "graticule")
		.attr("d", path);

var countries;

d3.json("/javascripts/world-50m.json", function(error, world) {
	countries = topojson.feature(world, world.objects.countries).features,
			neighbors = topojson.neighbors(world.objects.countries.geometries);
	
	svg.selectAll(".country")
			.data(countries)
		.enter().insert("path", ".graticule")
			.attr("class", "country")
			.attr("d", path)
			.style("fill", "#ffffff"); // Set color here
	
	// White border around the counties
	svg.insert("path", ".graticule")
			.datum(topojson.mesh(world, world.objects.countries, function(a, b) { return a !== b; }))
			.attr("class", "boundary")
			.attr("d", path);

	d3.select(self.frameElement).style("height", height + "px");

	socket.emit("fetch data", {})
});
