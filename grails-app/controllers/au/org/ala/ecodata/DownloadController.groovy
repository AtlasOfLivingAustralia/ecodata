package au.org.ala.ecodata

class DownloadController {
    def get(String id) {
        if (!id) {
            response.setStatus(400)
            render "A download ID is required"
        } else {
            String extension = params.format ?: 'zip'
            File file = new File("${grailsApplication.config.temp.dir}${File.separator}${id}.${extension}")
            if (file.exists()) {
                response.setContentType("application/zip")
                response.setHeader('Content-Disposition', 'Attachment;Filename="data.'+extension+'"')
                file.withInputStream { i -> response.outputStream << i }
            } else {
                response.setStatus(404)
                String msg = "No download was found for id: ${id}"
                msg += extension ? ' [Type:'+extension + ']' : ''
                render msg
            }
        }
    }
}
