package org.secuso.privacyfriendlyweather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import org.secuso.privacyfriendlyweather.R;
import org.secuso.privacyfriendlyweather.activities.ForecastCityActivity;
import org.secuso.privacyfriendlyweather.database.City;
import org.secuso.privacyfriendlyweather.preferences.AppPreferencesManager;
import org.secuso.privacyfriendlyweather.services.UpdateDataService;
import org.secuso.privacyfriendlyweather.ui.UiResourceProvider;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static android.support.v4.app.JobIntentService.enqueueWork;
import static org.secuso.privacyfriendlyweather.services.UpdateDataService.SKIP_UPDATE_INTERVAL;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WeatherWidgetConfigureActivity WeatherWidgetConfigureActivity}
 */
public class WeatherWidgetThreeDayForecast extends AppWidgetProvider {
    public static final String PREFS_NAME = "org.secuso.privacyfriendlyweather.widget.WeatherWidget3Day";

    public static void updateAppWidget(final Context context, final int appWidgetId) {

        // Construct the RemoteViews object
        Intent intent = new Intent(context, UpdateDataService.class);
        intent.setAction(UpdateDataService.UPDATE_WIDGET_ACTION);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("widget_type", 3);
        intent.putExtra(SKIP_UPDATE_INTERVAL, true);
        enqueueWork(context, UpdateDataService.class, 0, intent);

    }

    public static void updateView(Context context, AppWidgetManager appWidgetManager, RemoteViews views, int appWidgetId, float[][] data, City city) {
        AppPreferencesManager prefManager =
                new AppPreferencesManager(PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()));
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
        dayFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        //forecastList = DayForecastFilter.filter(forecastList, 3);
        if (data.length < 3) return;

        String day1 = dayFormat.format(data[0][8]);
        String day2 = dayFormat.format(data[1][8]);
        String day3 = dayFormat.format(data[2][8]);

        String temperature1 = String.format(
                "%s | %s%s",
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[0][0])),
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[0][1])),
                prefManager.getWeatherUnit()
        );
        String temperature2 = String.format(
                "%s | %s%s",
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[1][0])),
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[1][1])),
                prefManager.getWeatherUnit()
        );
        String temperature3 = String.format(
                "%s | %s%s",
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[2][0])),
                decimalFormat.format(prefManager.convertTemperatureFromCelsius(data[2][1])),
                prefManager.getWeatherUnit()
        );

        String hum1 = String.format("%s | %s%%", (int) data[0][2], (int) data[0][3]);
        String hum2 = String.format("%s | %s%%", (int) data[1][2], (int) data[1][3]);
        String hum3 = String.format("%s | %s%%", (int) data[2][2], (int) data[2][3]);

        views.setTextViewText(R.id.widget_city_name, city.getCityName());

        views.setTextViewText(R.id.widget_city_weather_3day_day1, day1);
        views.setTextViewText(R.id.widget_city_weather_3day_day2, day2);
        views.setTextViewText(R.id.widget_city_weather_3day_day3, day3);
        views.setTextViewText(R.id.widget_city_weather_3day_temp1, temperature1);
        views.setTextViewText(R.id.widget_city_weather_3day_temp2, temperature2);
        views.setTextViewText(R.id.widget_city_weather_3day_temp3, temperature3);
        views.setTextViewText(R.id.widget_city_weather_3day_hum1, hum1);
        views.setTextViewText(R.id.widget_city_weather_3day_hum2, hum2);
        views.setTextViewText(R.id.widget_city_weather_3day_hum3, hum3);

        views.setImageViewResource(R.id.widget_city_weather_3day_image1, UiResourceProvider.getIconResourceForWeatherCategory((int) data[0][9], true));
        views.setImageViewResource(R.id.widget_city_weather_3day_image2, UiResourceProvider.getIconResourceForWeatherCategory((int) data[1][9], true));
        views.setImageViewResource(R.id.widget_city_weather_3day_image3, UiResourceProvider.getIconResourceForWeatherCategory((int) data[2][9], true));

        Intent intent = new Intent(context, ForecastCityActivity.class);
        intent.putExtra("cityId", city.getCityId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.widget3day_layout, pendingIntent);
        
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            WeatherWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

