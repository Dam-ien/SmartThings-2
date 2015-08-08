/** mjpg_streamer IP Camera
 *
 * Author: rob.a.landry@gmail.com
 * Author: danny@smartthings.com
 * Author: brian@bevey.org
 * 
 * Author: http://github.com/egid - cleanup, better match the D-Link url format
 * Date: 5/21/14
 */

preferences
{
	input("username",	"text",		title: "Camera username",	description: "Username for web login")
	input("password",	"password",	title: "Camera password",	description: "Password for web login")
	input("url",		"text",		title: "IP or URL of camera",	description: "Do not include http://")
	input("port",		"text",		title: "Port",			description: "Port")
}

metadata {
	definition (name: "mjpg_streamer IP Camera", author: "Rob Landry", namespace: "roblandry") {
		capability "Image Capture"
		capability "Switch"
	}

	tiles {
		carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
			state "default", label: '', action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF"
		}

		standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
			state "take", label: 'Take Photo', action: "Image Capture.take", icon: "st.camera.take-photo", nextState:"taking"
		}

		/*standardTile("indicator", "device.indicatorStatus", inactiveLabel: false, decoration: "flat") {
			state "off", label: 'Switch Off', action:"Switch.off", icon:"st.switches.switch.off"
			state "on", label: 'Switch On', action:"Switch.off", icon:"st.switches.switch.on"
		}*/

		main "camera"
		details(["cameraDetails","take"])
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

	httpGet("http://${username}:${password}@${url}:${port}/?action=snapshot"){
		//httpGet("http://${username}:${password}@${url}:${port}/photo_save_only.jpg")
		//httpGet("http://${username}:${password}@${url}:${port}/photo_save_only.jpg")
		//httpGet("http://${username}:${password}@${url}:${port}/photo.jpg"){
			response -> log.info("${device.label} image captured")
			parseCameraResponse(response)
		//}
	}
}

/*def on() {
	httpGet("http://${username}:${password}@${url}:${port}/enabletorch"){
		response -> log.info("${device.label} Led On")
		sendEvent(name: "indicatorStatus", value: "on", display: false)
	}
}

def off() {
	httpGet("http://${username}:${password}@${url}:${port}/enabletorch"){
		response -> log.info("${device.label} Led On")
		sendEvent(name: "indicatorStatus", value: "off", display: false)
	}
}*/