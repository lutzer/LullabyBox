define([
	'jquery',
	'underscore',
	'marionette',
	'text!templates/mapTemplate.html'
], function($, _, Marionette, template){
	
	var MapView = Backbone.Marionette.ItemView.extend({
		
		initialize: function(options) {
			
			//create marker object that the collection can add to
			this.markers = new L.MarkerClusterGroup({
				maxClusterRadius : 25,
				showCoverageOnHover: false,
				spiderfyOnMaxZoom: true,
				spiderfyDistanceMultiplier: 2,
				iconCreateFunction: function(cluster) {
			        return new L.divIcon({ 
			        	html: cluster.getChildCount(),
			        	className: 'marker cluster',
			        	iconSize: [36,36],
						iconAnchor: [18, 18]
			        });
			    }
			});
			
			//register collection events
			this.model = options.model;this.collection = options.collection;
			this.collection.on('add', this.addFeature, this);
			this.collection.on('reset', this.onCollectionSync,this);
			
		},
		
		template: _.template(template),
		
		events: {
		},
		
		onShow: function() {
			this.map = L.map(this.$('#map')[0], {
				center: [ 52,16],
				maxZoom: 5,
				minZoom: 5,
				zoom: 5,
				zoomControl: false
			});
			
			// add custom map layer
			L.tileLayer('map_tiles/{z}/{x}/{y}.png', {
				attribution: '-',
				minZoom: 1,
				maxZoom: 8,
				//opacity: 0.5,
			}).addTo(this.map);
			
			this.markers.addTo(this.map);
			
			edgeMarker = L.edgeMarker({
				layerGroup: this.markers,
				icon : L.icon({ // style markers
					iconUrl : 'images/edge_arrow_brown.png',
					clickable: true,
					iconSize: [48,48],
					iconAnchor: [24, 24]
				})
			});
			
			edgeMarker.addTo(this.map);
			
		},
		

		onCollectionSync: function() {
			this.collection.each(this.addFeature,this);
		},
		
		addFeature: function(model) {
			var geoJson = model.toGeoJSON();
			if (geoJson) {
				var layer = L.geoJson(geoJson,this.getMarkerLayerOptions());
				this.markers.addLayer(layer);
			}
		},
		
		getMarkerLayerOptions: function() {
			var self = this;
			return {
				pointToLayer: function (feature, latlng) {
					return L.marker(latlng, {icon : L.divIcon({ // style markers
						iconSize: [36,36],
						iconAnchor: [18, 18],
						clickable: true,
						className: 'marker',
						id: feature.properties.idName,
						html: '-_-',
					})});
				},
				onEachFeature: function(feature, layer) { // connect to event
					if (feature.properties.type == 'marker')
						layer.on("click", function() {
							self.onMarkerClick(this,feature);
						});
				}
			};
		},
		
		onMarkerClick: function(clickevent) {
			var recording = this.collection.get(clickevent.feature.properties.id);
			this.trigger('show:recordingPopup',recording);
			this.map.panTo([recording.get('latitude'),recording.get('longitude')]);
		}
		
		
	});
	// Our module now returns our view
	return MapView;
	
});