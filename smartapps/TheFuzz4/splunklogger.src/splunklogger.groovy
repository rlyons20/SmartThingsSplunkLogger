/**
* Event Logger For Splunk
* This was originally created by Brian Keifer I modified it to work with the Splunk HTTP Event Collector
*
* Copyright 2015 Brian Keifer
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* 01-17-2016 Merged code from rlyons20 to splunk lock devices
* 12-04-2016 Fixed the results so that they not only spit out the results but they still also send it off to splunk like it should
* 02-16-2016 Added the ability for non-ssl/SSL
* 05-18-2016 Added the ability to log to splunk over the lan and 
* 10-24-2017 Added the code from Uto to log humidity readings and spelling fixes
* used adrabkin code fix for the length with local logging
*/

definition(
	name: "Splunk HTTP Event Logger",
	namespace: "thefuzz4",
	author: "Brian Keifer and Jason Hamilton",
	description: "Log SmartThings events to a Splunk HTTP Event Collector server",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

	section ("Log these devices:") {
		input  "accelerations",  "capability.accelerationSensor", title:"Acceleration Sensors", multiple: true, required: false
		input  "alarms",  "capability.alarm", title:"Alarms", multiple: true, required: false
		input  "batteries",  "capability.battery", title:"Batteries", multiple: true, required: false
		input  "button",  "capability.button", title:"Buttons", multiple: true, required: false
		input  "codetectors",  "capability.carbonMonoxideDetector", title:"CO Detectors", multiple: true, required: false
		input  "contacts",  "capability.contactSensor", title:"Contacts", multiple: true, required: false
		input  "energymeters",  "capability.energyMeter", title:"Energy Meters", multiple: true, required: false
		input  "humidities",  "capability.relativeHumidityMeasurement", title:"Humidities", multiple: true, required: false
		input  "illuminances",  "capability.illuminanceMeasurement", title:"Illuminance Sensors", multiple: true, required: false
		input  "indicators",  "capability.indicator", title:"Indicators", multiple: true, required: false
		input  "lockDevice",  "capability.lock", title:"Locks", multiple: true, required: false
		input  "motions",  "capability.motionSensor", title:"Motion Sensors", multiple: true, required: false
		input  "musicplayer",  "capability.musicPlayer", title:"Music Players", multiple: true, required: false
		input  "powermeters",  "capability.powerMeter", title:"Power Meters", multiple: true, required: false
		input  "presences",  "capability.presenceSensor", title:"Presence Sensors", multiple: true, required: false
		input  "smokedetectors",  "capability.smokeDetector", title:"Smoke Detectors", multiple: true, required: false
		input  "switches",  "capability.switch", title:"Switches", multiple: true, required: false
		input  "levels",  "capability.switchLevel", title:"Switch Levels", multiple: true, required: false
		input  "temperatures",  "capability.temperatureMeasurement", title:"Temperature Sensors", multiple: true, required: false
		input  "voltage",  "capability.voltageMeasurement", title:"Voltage Sensors", multiple: true, required: false
		input  "waterdetectors",  "capability.waterSensor", title:"Water Sensors", multiple: true, required: false
	}

	section ("Splunk Server") {
		input "use_local", "boolean", title: "Local Server?", required: true
		input "splunk_host", "text", title: "Splunk Hostname/IP", required: true
		input "use_ssl", "boolean", title: "Use SSL?", required: true
		input "splunk_port", "number", title: "Splunk Port", required: true
		input "splunk_token", "text", title: "Splunk Authentication Token", required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	doSubscriptions()
}

def doSubscriptions() {
	subscribe(accelerations,	"acceleration",			accelerationHandler)
	subscribe(alarms,		"alarm",			alarmHandler)
	subscribe(batteries,		"battery",			batteryHandler)
	subscribe(button,		"button",			buttonHandler)
	subscribe(codetectors,		"carbonMonoxideDetector",	coHandler)
	subscribe(contacts,		"contact",			contactHandler)
	subscribe(energymeters,		"energy",			energyHandler)
	subscribe(humidities,		"humidity",			humidityHandler)
	subscribe(illuminances,		"illuminance",			illuminanceHandler)
	subscribe(indicators,		"indicator",			indicatorHandler)
	subscribe(location,		"location",			locationHandler)
	subscribe(lockDevice,		"lock",				lockHandler)
	subscribe(modes,		"locationMode",			modeHandler)
	subscribe(motions,		"motion",			motionHandler)
	subscribe(musicplayers,		"music",			musicHandler)
	subscribe(powermeters,		"power",			powerHandler)
	subscribe(presences,		"presence",			presenceHandler)
	subscribe(relays,		"relaySwitch",			relayHandler)
	subscribe(smokedetectors,	"smokeDetector",		smokeHandler)
	subscribe(switches,		"switch",			switchHandler)
	subscribe(levels,		"level",			levelHandler)
	subscribe(temperatures,		"temperature",			temperatureHandler)
	subscribe(voltageMeasurement,	"voltage",			voltageHandler)
	subscribe(waterdetectors,	"water",			waterHandler)
}

def genericHandler(evt) {
	/*
	log.debug("------------------------------")
	log.debug("date: ${evt.date}")
	log.debug("name: ${evt.name}")
	log.debug("displayName: ${evt.displayName}")
	log.debug("device: ${evt.device}")
	log.debug("deviceId: ${evt.deviceId}")
	log.debug("value: ${evt.value}")
	log.debug("isStateChange: ${evt.isStateChange()}")
	log.debug("id: ${evt.id}")
	log.debug("description: ${evt.description}")
	log.debug("descriptionText: ${evt.descriptionText}")
	log.debug("installedSmartAppId: ${evt.installedSmartAppId}")
	log.debug("isoDate: ${evt.isoDate}")
	log.debug("isDigital: ${evt.isDigital()}")
	log.debug("isPhysical: ${evt.isPhysical()}")
	log.debug("location: ${evt.location}")
	log.debug("locationId: ${evt.locationId}")
	log.debug("source: ${evt.source}")
	log.debug("unit: ${evt.unit}")
	*/

	def json = ""
	json += "{\"event\":"
	json += "{\"date\":\"${evt.date}\","
	json += "\"name\":\"${evt.name}\","
	json += "\"displayName\":\"${evt.displayName}\","
	json += "\"device\":\"${evt.device}\","
	json += "\"deviceId\":\"${evt.deviceId}\","
	json += "\"value\":\"${evt.value}\","
	json += "\"isStateChange\":\"${evt.isStateChange()}\","
	json += "\"id\":\"${evt.id}\","
	json += "\"description\":\"${evt.description}\","
	json += "\"descriptionText\":\"${evt.descriptionText}\","
	json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
	json += "\"isoDate\":\"${evt.isoDate}\","
	json += "\"isDigital\":\"${evt.isDigital()}\","
	json += "\"isPhysical\":\"${evt.isPhysical()}\","
	json += "\"location\":\"${evt.location}\","
	json += "\"locationId\":\"${evt.locationId}\","
	json += "\"unit\":\"${evt.unit}\","
	json += "\"source\":\"${evt.source}\",}"
	json += "}"
	//log.debug("JSON: ${json}")
	def ssl = use_ssl.toBoolean()
	def local = use_local.toBoolean()
	def http_protocol
	def splunk_server = "${splunk_host}:${splunk_port}"
	def length = json.getBytes().size().toString()
	def msg = parseLanMessage(description)
	def body = msg.body
	def status = msg.status

	if (local == true) {
		//sendHubCommand(new physicalgraph.device.HubAction([
		def result = (new physicalgraph.device.HubAction([
		method: "POST",
		path: "/services/collector/event",
		headers: [
		'Authorization': "Splunk ${splunk_token}",
		"Content-Length":"${length}",
		HOST: "${splunk_server}",
		"Content-Type":"application/json",
		"Accept-Encoding":"gzip,deflate"
		],
		body:json
		]))
		log.debug result
		sendHubCommand(result);
		return result
	}
	
	else {
		//log.debug "Use Remote"
		//log.debug "Current SSL Value ${use_ssl}"
		if (ssl == true) {
			//log.debug "Using SSL"
			http_protocol = "https"
		}
	else {
		//log.debug "Not Using SSL"
		http_protocol = "http"
	}

	def params = [
	uri: "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event", headers: [ 'Authorization': "Splunk ${splunk_token}" ],
		body: json ] log.debug params try { httpPostJson(params) } catch ( groovyx.net.http.HttpResponseException ex ) {
		log.debug "Unexpected response error: ${ex.statusCode}" 
		}
	}
}

def accelerationHandler(evt) {
	genericHandler(evt)
}

def alarmHandler(evt) {
	genericHandler(evt)
}

def batteryHandler(evt) {
	log.trace "$evt"
    def json = ""
	json += "{\"event\":"
	json += "{\"date\":\"${evt.date}\","
	json += "\"name\":\"${evt.name}\","
	json += "\"displayName\":\"${evt.displayName}\","
	json += "\"device\":\"${evt.device}\","
	json += "\"deviceId\":\"${evt.deviceId}\","
	json += "\"value\":\"${evt.value}\","
	json += "\"isStateChange\":\"${evt.isStateChange()}\","
	json += "\"id\":\"${evt.id}\","
	json += "\"description\":\"${evt.description}\","
	json += "\"descriptionText\":\"${evt.descriptionText}\","
	json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
	json += "\"isoDate\":\"${evt.isoDate}\","
	json += "\"isDigital\":\"${evt.isDigital()}\","
	json += "\"isPhysical\":\"${evt.isPhysical()}\","
	json += "\"location\":\"${evt.location}\","
	json += "\"locationId\":\"${evt.locationId}\","
	json += "\"unit\":\"${evt.unit}\","
	json += "\"source\":\"${evt.source}\",}"
	json += "}"
	//log.debug("JSON: ${json}")
	def ssl = use_ssl.toBoolean()
	def local = use_local.toBoolean()
	def http_protocol
	def splunk_server = "${splunk_host}:${splunk_port}"
	def length = json.getBytes().size().toString()
	def msg = parseLanMessage(description)
	def body = msg.body
	def status = msg.status

	if (local == true) {
		//sendHubCommand(new physicalgraph.device.HubAction([
		def result = (new physicalgraph.device.HubAction([
		method: "POST",
		path: "/services/collector/event",
		headers: [
		'Authorization': "Splunk ${splunk_token}",
		"Content-Length":"${length}",
		HOST: "${splunk_server}",
		"Content-Type":"application/json",
		"Accept-Encoding":"gzip,deflate"
		],
		body:json
		]))
		log.debug result
		sendHubCommand(result);
		return result
	}
	
	else {
		//log.debug "Use Remote"
		//log.debug "Current SSL Value ${use_ssl}"
		if (ssl == true) {
			//log.debug "Using SSL"
			http_protocol = "https"
		}
	else {
		//log.debug "Not Using SSL"
		http_protocol = "http"
	}

	def params = [
	uri: "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event", headers: [ 'Authorization': "Splunk ${splunk_token}" ],
		body: json ] log.debug params try { httpPostJson(params) } catch ( groovyx.net.http.HttpResponseException ex ) {
		log.debug "Unexpected response error: ${ex.statusCode}" 
		}
	}
}

def buttonHandler(evt) {
	genericHandler(evt)
}

def coHandler(evt) {
	genericHandler(evt)
}

def contactHandler(evt) {
	genericHandler(evt)
}

def energyHandler(evt) {
	genericHandler(evt)
}

def humidityHandler(evt) {
	genericHandler(evt)
}

def illuminanceHandler(evt) {
	genericHandler(evt)
}

def indicatorHandler(evt) {
	genericHandler(evt)
}

def locationHandler(evt) {
	genericHandler(evt)
}

def lockHandler(evt) {
	log.trace "$evt"
    def json = ""
	json += "{\"event\":"
	json += "{\"date\":\"${evt.date}\","
	json += "\"name\":\"${evt.name}\","
	json += "\"displayName\":\"${evt.displayName}\","
	json += "\"device\":\"${evt.device}\","
	json += "\"deviceId\":\"${evt.deviceId}\","
	json += "\"value\":\"${evt.value}\","
	json += "\"isStateChange\":\"${evt.isStateChange()}\","
	json += "\"id\":\"${evt.id}\","
	json += "\"description\":\"${evt.description}\","
	json += "\"descriptionText\":\"${evt.descriptionText}\","
	json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
	json += "\"isoDate\":\"${evt.isoDate}\","
	json += "\"isDigital\":\"${evt.isDigital()}\","
	json += "\"isPhysical\":\"${evt.isPhysical()}\","
	json += "\"location\":\"${evt.location}\","
	json += "\"locationId\":\"${evt.locationId}\","
	json += "\"unit\":\"${evt.unit}\","
	json += "\"source\":\"${evt.source}\",}"
	json += "}"
	//log.debug("JSON: ${json}")
	def ssl = use_ssl.toBoolean()
	def local = use_local.toBoolean()
	def http_protocol
	def splunk_server = "${splunk_host}:${splunk_port}"
	def length = json.getBytes().size().toString()
	def msg = parseLanMessage(description)
	def body = msg.body
	def status = msg.status

	if (local == true) {
		//sendHubCommand(new physicalgraph.device.HubAction([
		def result = (new physicalgraph.device.HubAction([
		method: "POST",
		path: "/services/collector/event",
		headers: [
		'Authorization': "Splunk ${splunk_token}",
		"Content-Length":"${length}",
		HOST: "${splunk_server}",
		"Content-Type":"application/json",
		"Accept-Encoding":"gzip,deflate"
		],
		body:json
		]))
		log.debug result
		sendHubCommand(result);
		return result
	}
	
	else {
		//log.debug "Use Remote"
		//log.debug "Current SSL Value ${use_ssl}"
		if (ssl == true) {
			//log.debug "Using SSL"
			http_protocol = "https"
		}
	else {
		//log.debug "Not Using SSL"
		http_protocol = "http"
	}

	def params = [
	uri: "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event", headers: [ 'Authorization': "Splunk ${splunk_token}" ],
		body: json ] log.debug params try { httpPostJson(params) } catch ( groovyx.net.http.HttpResponseException ex ) {
		log.debug "Unexpected response error: ${ex.statusCode}" 
		}
	}
}

def modeHandler(evt) {
	genericHandler(evt)
}

def motionHandler(evt) {
	genericHandler(evt)
}

def musicHandler(evt) {
	genericHandler(evt)
}

def powerHandler(evt) {
	genericHandler(evt)
}

def presenceHandler(evt) {
	genericHandler(evt)
}

def relayHandler(evt) {
	genericHandler(evt)
}

def smokeHandler(evt) {
	genericHandler(evt)
}

def switchHandler(evt) {
	genericHandler(evt)
}

def levelHandler(evt) {
	genericHandler(evt)
}

def temperatureHandler(evt) {
	genericHandler(evt)
}

def voltageHandler(evt) {
	genericHandler(evt)
}

def waterHandler(evt) {
	genericHandler(evt)
}
