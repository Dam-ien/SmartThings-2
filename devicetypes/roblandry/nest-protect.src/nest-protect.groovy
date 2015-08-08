/**
 *  Nest Protect
 *  Author: rob.a.landry@gmail.com
 */

preferences {
    input("username", "text",     title: "Username",    description: "Your Nest username (usually an email address)")
    input("password", "password", title: "Password",    description: "Your Nest password")
    input("mac",      "text",     title: "MAC Address", description: "The MAC address of your smoke detector")
}

 // for the UI
metadata {
	definition (name: "Nest Protect", namespace: "roblandry", author: "Rob Landry") {
		capability "Smoke Detector"
		capability "Carbon Monoxide Detector"
		capability "Sensor"
		capability "Battery"
		capability "Polling"
		command "refresh"
		command "pollScheduleOn"
		command "pollScheduleOff"

		fingerprint deviceId: "0xA100", inClusters: "0x20,0x80,0x70,0x85,0x71,0x72,0x86"
	}

	simulator {
		status "smoke": "command: 7105, payload: 01 FF"
		status "clear": "command: 7105, payload: 01 00"
		status "test": "command: 7105, payload: 0C FF"
		status "carbonMonoxide": "command: 7105, payload: 02 FF"
		status "carbonMonoxide clear": "command: 7105, payload: 02 00"
		status "battery 100%": "command: 8003, payload: 64"
		status "battery 5%": "command: 8003, payload: 05"
	}
 
	tiles {

		valueTile("smoke", "device.smoke", width: 2, height: 2){
			state("clear",    label:"Smoke\n Clear",     icon:"st.alarm.smoke.clear", backgroundColor:"#44B621")
			state("detected", label:"Smoke\n Detected!", icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")
			state("tested",   label:"Smoke\n Tested",    icon:"st.alarm.smoke.test",  backgroundColor:"#e86d13")
		}

		valueTile("carbonMonoxide", "device.carbonMonoxide"){
			state("clear",    label:"CO\n Clear",     icon:"st.particulate.particulate.particulate", backgroundColor:"#44B621")
			state("detected", label:"CO\n Detected!", icon:"st.particulate.particulate.particulate", backgroundColor:"#e86d13")
			state("tested",   label:"CO\n Tested",    icon:"st.particulate.particulate.particulate", backgroundColor:"#e86d13")
		}

		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state("battery", label:'${currentValue}%\n Battery', icon:"", 
				backgroundColors:[
					[value: 4, color: "#FF0000"],
					[value: 19, color: "#FFA500"],
					[value: 49, color: "#FFFF00"],
					[value: 100, color: "#44B621"]
				]
			)
		}
		/*valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state("battery100", label:'${currentValue}%\n Battery', icon:"http://cdn.flaticon.com/png/256/63332.png", backgroundColor:"#44B621")
			state("battery49", label:'${currentValue}%\n Battery', icon:"http://cdn.flaticon.com/png/256/63256.png", backgroundColor:"#FFFF00")
			state("battery19", label:'${currentValue}%\n Battery', icon:"http://cdn.flaticon.com/png/256/63344.png", backgroundColor:"#FFA500")
			state("battery4", label:'${currentValue}%\n Battery', icon:"http://cdn.flaticon.com/png/256/63279.png", backgroundColor:"#FF0000")
		}*/

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state("default", label:'', action:"refresh", icon:"st.secondary.refresh")
		}

		standardTile("polling", "device.polling", inactiveLabel: false, decoration: "flat") {
			state("pollingOff", label:'Poll Off', action:"pollScheduleOn",  icon:"st.secondary.refresh-icon")
			state("pollingOn",  label:'Poll On',  action:"pollScheduleOff", icon:"st.secondary.refresh-icon", backgroundColor:"#44B621")
		}

		main "smoke"
		details(["smoke", "carbonMonoxide", "battery", "refresh", "polling"])
	}
}
 

def parse(String description) {}

def configure() {}

def refresh() { poll() }

def poll() {
	def smoke_status
	def co_status
	def battery_health_state
	def battery_level
    def batteryHackName

	log.info "Executing 'poll'"
	api('status') {
		data.topaz = it.data.topaz.getAt(settings.mac.toUpperCase())
		smoke_status = data.topaz.smoke_status == 0? "clear" : "detected"
		co_status = data.topaz.co_status == 0? "clear" : "detected"
		//battery_health_state = data.topaz.battery_health_state  == 0 ? "OK" : "Low"
		battery_level = batVoltValue(data.topaz.battery_level)

		//batteryHackName = batteryHack(battery_level as Integer)
		sendEvent(name: 'smoke', value: "${smoke_status}")
		sendEvent(name: 'carbonMonoxide', value: "${co_status}")
		//sendEvent(name: batteryHackName, value: battery_level as Integer)
        //sendEvent(name: 'battery', value: "battery19")
        sendEvent(name: 'battery', value: battery_level as Integer)

		nestDataReturned(data)
	}
}

def api(method, success = {}) {
	if(!isLoggedIn()) {
		log.info "Need to login"
		login(method, args, success)
		return
	}

	log.info "Logged in"

	doRequest("/v2/mobile/${data.auth.user}", "get", success)
}

// Need to be logged in before this is called. So don't call this. Call api.
def doRequest(uri, type, success) {
	log.debug "Calling $type : $uri : $args"

	if(uri.charAt(0) == '/') {
		uri = "${data.auth.urls.transport_url}${uri}"
	}

	def params = [
		uri: uri,
		headers: [
			'X-nl-protocol-version': 1,
			'X-nl-user-id': data.auth.userid,
			'Authorization': "Basic ${data.auth.access_token}"
		],
		body: args
	]

	try {
		if(type == 'post') {
			httpPostJson(params, success)
		}

		else if (type == 'get') {
			httpGet(params, success)
		}
	} catch (Throwable e) {
		login()
	}
}

def login(method = null, args = [], success = {}) {
	def params = [
		uri: 'https://home.nest.com/user/login',
		body: [username: settings.username, password: settings.password]
	]

	httpPost(params) {response ->
		data.auth = response.data
		data.auth.expires_in = Date.parse('EEE, dd-MMM-yyyy HH:mm:ss z', response.data.expires_in).getTime()
		//log.debug data.auth

		api(method, success)
	}
}

def isLoggedIn() {
	if(!data.auth) {
		log.debug "No data.auth"
		return false
	}

	def now = new Date().getTime();
	return data.auth.expires_in > now
}

def nestDataReturned(data) {
	log.trace settings.mac
	data.topaz.each { key,value -> log.trace "${key}: ${value}" }
}

// http://stackoverflow.com/a/929107
def batVoltValue(OldValue) {
	def OldMax = 5500 //Full Battery Voltage?
	def OldMin = 4200 //Dead Battery Voltage?
	def NewMax = 100
	def NewMin = 0
	def OldRange = (OldMax - OldMin)  
	def NewRange = (NewMax - NewMin)  
	def NewValue = (((OldValue - OldMin) * NewRange) / OldRange) + NewMin

	return NewValue
}

def pollScheduleOn() {
	log.info "Creating Polling Schedule."
	unschedule("poll")
	schedule("1 * * * * ?", poll)
	sendEvent(name: 'polling', value: "pollingOn")
}

def pollScheduleOff() {
	log.info "Removing Polling Schedule."
	unschedule("poll")
	sendEvent(name: 'polling', value: "pollingOff")
}

def batteryHack(batValue) {
	def retValue
    if                      ( batValue > 50) { retValue = "battery100" }
    else if (batValue < 50 && batValue > 20) { retValue = "battery49" }
    else if (batValue < 20 && batValue >  5) { retValue = "battery9" }
    else if (batValue <  5 )                 { retValue = "battery4" }
    return retValue
}