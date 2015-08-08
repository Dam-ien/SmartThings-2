/** D-Link Camera
 *
 *  Author: Rob Landry
 * 
 *  URL: http://github.com/roblandry/android-ip-camera.device
 * 
 *  Date: 3/6/15
 *  
 *  Version: 1.0.1
 * 
 *  Description: This is a custom device type. This works with the Android IP Camera app. It allows you to take photos, 
 *  record video, turn on/off the led, focus, overlay, and night vision. It displays various sensors including battery 
 *  level, humidity, temperature, and light (lux). The sensor data is all dependent on what your phone supports.
 * 
 *  Copyright: 2015 Rob Landry
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

preferences
{
	input("username",	"text",		title: "Camera username",	description: "Username for web login")
	input("password",	"password",	title: "Camera password",	description: "Password for web login")
	input("url",		"text",		title: "IP or URL of camera",	description: "Do not include http://")
	input("port",		"text",		title: "Port",			description: "Port")
}

metadata {
	definition (name: "D-Link Camera", namespace: "roblandry", author: "Rob Landry") {
		capability "Image Capture"
		capability "Switch"
		capability "Actuator"
		//capability "Battery"
		//capability "Illuminance Measurement"
		//capability "Temperature Measurement"
		//capability "Relative Humidity Measurement"

		command "ledOn"
		command "ledOff"
		//command "focusOn"
		//command "focusOff"
		//command "overlayOn"
		//command "overlayOff"
		//command "nightVisionOn"
		//command "nightVisionOff"
		command "motionDetectionOn"
		command "motionDetectionOff"
		command "pirDetectionOn"
		command "pirDetectionOff"
		command "refresh"


	}

	tiles {
		carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
			state("default", label: '', action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF")
		}

		standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
			state("take", label: 'Take Photo', action: "Image Capture.take", icon: "st.camera.take-photo", nextState:"taking")
			state("taking", label: 'Taking...', action: "Image Capture.take", icon: "st.camera.take-photo", backgroundColor: "#79b821")
		}

		standardTile("motionDetection", "device.motion", width: 1, height: 1) {
			state("motionDetectionOff", label: 'motionDetection Off', action:"motionDetectionOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("motionDetectionOn", label: 'motionDetection On', action:"motionDetectionOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("pirDetection", "device.nightVision", width: 1, height: 1) {
			state("pirDetectionOff", label: 'PIR Off', action:"pirDetectionOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("pirDetectionOn", label: 'PIR On', action:"pirDetectionOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		/*standardTile("record", "device.switch", width: 1, height: 1) {
			state("recordOff", label: 'Record Off', action:"switch.on", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("recordOn", label: 'Record On', action:"switch.off", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}*/

		standardTile("led", "device.led", width: 1, height: 1) {
			state("ledOff", label: 'Led Off', action:"ledOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("ledOn", label: 'Led On', action:"ledOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state("default", label:"", action:"refresh", icon:"st.secondary.refresh")
		}

		main "camera"
		details(["cameraDetails","take","led","motionDetection","pirDetection","refresh"/*"record","focus","overlay","nightVision","battery","temperature","light","humidity",*/])
	}
}


def parseCameraResponse(def response) {
	if(response.headers.'Content-Type'.contains("image/jpeg")) {
		def imageBytes = response.data

		if(imageBytes) {
			storeImage(getPictureName(), imageBytes)
		}
	} else {
		log.error("${device.label} could not capture an image.")
	}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	"image" + "_$pictureUuid" + ".jpg"
}

private take() {
	log.info("${device.label} taking photo")

	httpGet("http://${username}:${password}@${url}:${port}/image/jpeg.cgi"){
		response -> log.info("${device.label} image captured")
		parseCameraResponse(response)
	}
}

def on(theSwitch="record") {
log.debug "theSwitch: ${theSwitch}, ${username}, ${password}"
	def sUrl
	switch ( theSwitch ) {
		case "led":
			sUrl = "config/led.cgi?led=on"
			break

		case "motionDetection":
			sUrl = "config/motion.cgi?enable=yes"
			break

		case "pirDetection":
			sUrl = "config/motion.cgi?pir=yes"
			break

		default:
			sUrl = "/startvideo?force=1"
	}

	httpGet("http://${username}:${password}@${url}:${port}/${sUrl}"){
		response -> log.info("${device.label} ${theSwitch} On")
		sendEvent(name: "${theSwitch}", value: "${theSwitch}On")
	}

}

def off(theSwitch="record") {
	def sUrl
	switch ( theSwitch ) {
		case "led":
			sUrl = "config/led.cgi?led=off"
			break

		case "motionDetection":
			sUrl = "config/motion.cgi/?enable=no"
			break

		case "pirDetection":
			sUrl = "config/motion.cgi?pir=no"
			break

		default:
			sUrl = "stopvideo?force=1"
	}

	httpGet("http://${username}:${password}@${url}:${port}/${sUrl}"){
		response -> log.info("${device.label} ${theSwitch} Off")
		sendEvent(name: "${theSwitch}", value: "${theSwitch}Off")
	}

}

def ledOn() { on("led") }

def ledOff() { off("led") }

def motionDetectionOn() { on("motionDetection") }

def motionDetectionOff() { off("motionDetection") }

def pirDetectionOn() { on("pirDetection") }

def pirDetectionOff() { off("pirDetection") }

def installed() {  }

def configure() { poll() }

def poll() { refresh() }

def refresh() { //getDefaults() 
}

def getDefaults() {
//httpget crash http://community.smartthings.com/t/authenticate-foscam-ip-camera-for-http-post-commands/579/36
	def sUrl
	def theStatus = [ "led" : "config/led.cgi", "motionDetection" : "config/motion.cgi/" ]

	theStatus.each {
		log.debug "${it.key} ${it.value}"

		httpGet("http://${username}:${password}@${url}:${port}/${it.value}"){
			response -> log.debug response.data

            if (response.data == "led=on") {log.info "led=on"}
			//log.info("${device.label} ${it.key}${it.value.capitalize()}")
			//sendEvent(name: "${it.key}", value: "${it.key}${it.value.capitalize()}")
		}
	}


}