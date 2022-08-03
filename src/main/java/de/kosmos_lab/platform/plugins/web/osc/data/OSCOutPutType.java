package de.kosmos_lab.platform.plugins.web.osc.data;

public enum OSCOutPutType {
    MAIN(new String[]{"/mix/fader"}),
    STEREO_AUX_1(new String[]{"/01/level", "/02/level"}),
    STEREO_AUX_2(new String[]{"/03/level", "/04/level"}),
    STEREO_AUX_3(new String[]{"/05/level", "/06/level"}),
    AUX_1(new String[]{"/01/level"}),
    AUX_2(new String[]{"/02/level"}),
    AUX_3(new String[]{"/03/level"}),
    AUX_4(new String[]{"/04/level"}),
    AUX_5(new String[]{"/05/level"}),
    AUX_6(new String[]{"/06/level"});

    private final String paths[];

    OSCOutPutType(String[] paths) {
        this.paths = paths;
    }
}
