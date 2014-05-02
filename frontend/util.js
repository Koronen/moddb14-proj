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

function printColor(color, per){
	var array = per2RGBArray(per);
	console.log(color + "\t" + array + "  \t" + RGB2Str(array));
}

/*printColor("blue", 0.0);
printColor("teal", 0.2);
printColor("green", 0.4);
printColor("yellow", 0.6);
printColor("orange", 0.8);
printColor("red", 1.0);
*/

module.exports = {
	RGB2Str: RGB2Str,
	per2RGBArray: per2RGBArray
}
