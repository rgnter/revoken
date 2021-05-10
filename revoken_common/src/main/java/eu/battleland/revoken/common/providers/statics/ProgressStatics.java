package eu.battleland.revoken.common.providers.statics;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

public class ProgressStatics {


    @Builder
    public static class BarSettings {
        public int stepMin = 0;
        public int stepMax = 10;

        public final String visualBorder             = "§8┃";
        public final String visualCompletedStep      = "§a░";
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
