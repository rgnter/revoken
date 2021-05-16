package eu.battleland.revoken.common.providers.statics;

import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.data.codec.meta.CodecKey;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

public class ProgressStatics {


    @Builder
    public static class BarSettings implements ICodec {
        @CodecKey("progress.step-min")
        public int stepMin = 0;
        @CodecKey("progress.step-max")
        public int stepMax = 10;

        @CodecKey("progress.visual.border")
        public final String visualBorder             = "§8┃";
        @CodecKey("progress.visual.completed-step")
        public final String visualCompletedStep      = "§a░";
        @CodecKey("progress.visual.uncompleted-step")
        public final String visualUncompletedStep    = "§7░";

    }



    /**
     *
     * @param current Current progress
     * @param target  Targeted progress
     * @return Coloured progress bar
     */
    public static @NotNull String getProgressBar(@NotNull BarSettings settings, double current, double target) {
        int currentStep = 0;

        if(current != 0 || target != 0)
            currentStep = (int) Math.round(settings.stepMax * (current / target));

        StringBuilder bar = new StringBuilder(settings.visualBorder);
        for(int step = settings.stepMin; step < settings.stepMax; step++) {
            // step is completed
            if(step <= currentStep) {
                bar.append(settings.visualCompletedStep);
            } else
                bar.append(settings.visualUncompletedStep);
        }
        bar.append(settings.visualBorder);
        return bar.toString();
    }

}
