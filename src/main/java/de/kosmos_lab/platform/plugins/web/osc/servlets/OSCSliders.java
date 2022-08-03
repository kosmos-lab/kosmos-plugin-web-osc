package de.kosmos_lab.platform.plugins.web.osc.servlets;

import de.kosmos_lab.kosmos.annotations.Operation;
import de.kosmos_lab.kosmos.annotations.enums.SchemaType;
import de.kosmos_lab.kosmos.annotations.media.Content;
import de.kosmos_lab.kosmos.annotations.media.Schema;
import de.kosmos_lab.kosmos.annotations.responses.ApiResponse;
import de.kosmos_lab.kosmos.doc.openapi.ApiEndpoint;
import de.kosmos_lab.kosmos.doc.openapi.ResponseCode;
import de.kosmos_lab.kosmos.platform.IController;
import de.kosmos_lab.kosmos.platform.web.KosmoSHttpServletRequest;
import de.kosmos_lab.kosmos.platform.web.WebServer;
import de.kosmos_lab.kosmos.platform.web.servlets.AuthedServlet;
import de.kosmos_lab.kosmos.platform.web.servlets.KosmoSServlet;
import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.io.IOException;

@Extension
@ApiEndpoint(
        path = "/osc/sliders",
        userLevel = 1
)
public class OSCSliders extends AuthedServlet implements ExtensionPoint {


    private final OSCController osc;

    public OSCSliders(WebServer webServer, IController controller, int level) {
        super(webServer, controller, level);
        this.osc = OSCController.getInstance(controller);
    }

    @Operation(

            tags = {"kiosk"},

            description = "Get the pre defined objects for the kiosk UI",
            summary = "get objects",


            responses = {
                    @ApiResponse(
                            responseCode = @ResponseCode(statusCode = KosmoSServlet.STATUS_OK),
                            description = "the predefined objects",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(type = SchemaType.ARRAY))),
                    @ApiResponse(responseCode = @ResponseCode(statusCode = KosmoSServlet.STATUS_ERROR), ref = "#/components/responses/UnknownError"),
            }
    )
    public void get(KosmoSHttpServletRequest request, HttpServletResponse response)

            throws IOException {


        sendJSON(request, response, osc.getSliders());

    }


}

