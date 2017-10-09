function initMap() {
	// Create a map object and specify the DOM element for display.
	var mapElement = document.getElementById('map'), map = new google.maps.Map(mapElement, {
		center : {
			lat : parseFloat(mapElement.getAttribute("data-lat")),
			lng : parseFloat(mapElement.getAttribute("data-lon"))
		},
		zoom : 10
	});

	var flights = {};

	function update() {
		$.ajax({
			url : "/admin/adsb/data.json",
			headers: {          
			    Accept: "application/json; charset=utf-8"         
			},			
			success : function(data) {
				
				var existingFlights = {};
				
				for ( var i in data) {
					var $path = [];
					for ( var j in data[i].positions) {
						$path.push(new google.maps.LatLng(data[i].positions[j].latitude, data[i].positions[j].longitude));
					}
					
					existingFlights[data[i].icao24]=true;

					var color;
					if (data[i].icao24 in flights) {
						flights[data[i].icao24]["line"].setPath($path);
						flights[data[i].icao24]["marker"].setPosition($path[$path.length - 1]);
						flights[data[i].icao24]["marker"].getIcon().url = RotateIcon.makeIcon('/img/airplane.png').setRotation({
							deg : calculateRotationDegree($path)
						}).getUrl();
					} else {
						color = getRandomColor();
						var line = new google.maps.Polyline({
							path : $path,
							strokeColor : color,
							strokeOpacity : 1.0,
							strokeWeight : 1,
							map : map
						});
						var marker = new google.maps.Marker({
							icon : {
								url : RotateIcon.makeIcon('/img/airplane.png').setRotation({
									deg : calculateRotationDegree($path)
								}).getUrl(),
								anchor : new google.maps.Point(8, 8)
							},
							position : $path[$path.length - 1],
							map : map,
							title : data[i].icao24
						});

						flights[data[i].icao24] = {
							"line" : line,
							"marker" : marker
						};
					}

				}
				
				for( var key in flights ) {
					if( !(key in existingFlights)  ) {
						flights[key]["line"].setMap(null);
						flights[key]["marker"].setMap(null);
						delete flights[key];
					}
				}

			},
			complete : function() {
				setTimeout(update, 1000);
			}
		});
	}

	setTimeout(update, 1000);

	$(document).ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
		if (jqXHR.status == 401) {
			location.href = "/";
		}
	});

	function calculateRotationDegree($path) {
		if ($path.length < 2) {
			return 0;
		}
		var y = $path[$path.length - 1].lat() - $path[$path.length - 2].lat();
		var x = $path[$path.length - 1].lng() - $path[$path.length - 2].lng();
		var degree = Math.atan(Math.abs(y / x)) * 180 / Math.PI;

		var result;
		if (y < 0 && x > 0) {
			result = 90 + degree;
		} else if (y < 0 && x < 0) {
			result = 270 - degree;
		} else if (y > 0 && x < 0) {
			result = 360 - degree;
		} else {
			result = 90 - degree;
		}

		return result;

	}

	function getRandomColor() {
		var letters = '0123456789ABCDEF';
		var color = '#';
		for (var i = 0; i < 6; i++) {
			color += letters[Math.floor(Math.random() * 16)];
		}
		return color;
	}
}

var RotateIcon = function(options) {
	this.options = options || {};
	this.rImg = options.img || new Image();
	this.rImg.src = this.rImg.src || this.options.url || '';
	this.options.width = this.options.width || this.rImg.width || 52;
	this.options.height = this.options.height || this.rImg.height || 60;
	var canvas = document.createElement("canvas");
	canvas.width = this.options.width;
	canvas.height = this.options.height;
	this.context = canvas.getContext("2d");
	this.canvas = canvas;
};
RotateIcon.makeIcon = function(url) {
	return new RotateIcon({
		url : url
	});
};
RotateIcon.prototype.setRotation = function(options) {
	var canvas = this.context, angle = options.deg ? options.deg * Math.PI / 180 : options.rad, centerX = this.options.width / 2, centerY = this.options.height / 2;

	canvas.clearRect(0, 0, this.options.width, this.options.height);
	canvas.save();
	canvas.translate(centerX, centerY);
	canvas.rotate(angle);
	canvas.translate(-centerX, -centerY);
	canvas.drawImage(this.rImg, 0, 0);
	canvas.restore();
	return this;
};
RotateIcon.prototype.getUrl = function() {
	return this.canvas.toDataURL('image/png');
};
