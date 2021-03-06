package org.secuso.privacyfriendlyweather.ui.RecycleList;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.secuso.privacyfriendlyweather.R;
import org.secuso.privacyfriendlyweather.database.CurrentWeatherData;
import org.secuso.privacyfriendlyweather.database.Forecast;
import org.secuso.privacyfriendlyweather.database.PFASQLiteHelper;
import org.secuso.privacyfriendlyweather.ui.Help.StringFormatUtils;
import org.secuso.privacyfriendlyweather.ui.UiResourceProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class CityWeatherAdapter extends RecyclerView.Adapter<CityWeatherAdapter.ViewHolder> {
    private static final String TAG = "Forecast_Adapter";

    private int[] dataSetTypes;
    private List<Forecast> courseDayList;
    private float[][] forecastData;

    private Context context;

    private CurrentWeatherData currentWeatherDataList;

    public static final int OVERVIEW = 0;
    public static final int DETAILS = 1;
    public static final int WEEK = 2;
    public static final int DAY = 3;
    public static final int SUN = 4;
    public static final int ERROR = 5;

    public CityWeatherAdapter(CurrentWeatherData currentWeatherDataList, int[] dataSetTypes, Context context) {
        this.currentWeatherDataList = currentWeatherDataList;
        this.dataSetTypes = dataSetTypes;
        this.context = context;

        PFASQLiteHelper database = PFASQLiteHelper.getInstance(context.getApplicationContext());

        List<Forecast> forecasts = database.getForecastsByCityId(currentWeatherDataList.getCity_id());

        updateForecastData(forecasts);
    }

    // function for 3-hour forecast list
    public void updateForecastData(List<Forecast> forecasts) {
        //Log.d("forecast", "in cityweatheradapter " + forecasts.get(0).getCity_id() + " " + forecasts.size() + " " + forecasts.get(0).getForecastTime());
        forecastData = compressWeatherData(forecasts);
        courseDayList = new ArrayList<Forecast>();

        // TODO: filter them accordingly and calculate what should be displayed .. (like average all the 3h forecasts for the week list)
        long threehoursago = System.currentTimeMillis() - (3 * 60 * 60 * 1000);

        for (Forecast f : forecasts) {

            // only add Forecasts that are in the future
            if (f.getForecastTime() >= threehoursago) {
                // course of day list should show entries until the same time the next day is reached
                // since we force our forecasts to be in the future and they are ordered.. we can assume
                // the next entry to be to the full 3h mark after this time ..
                // if we now add a total of 24 entries if should sum up to 72 hours
                if (courseDayList.size() < 25) {
                    courseDayList.add(f);
                }
            }
        }
        //TODO update Titlebar text
        notifyDataSetChanged();
    }

    private float[][] compressWeatherData(List<Forecast> forecastList) {
        if (forecastList.isEmpty()) {
            Log.d("devtag", "######## forecastlist empty");
            return new float[][]{new float[]{0}};
        }
        int cityId = forecastList.get(0).getCity_id();

        PFASQLiteHelper dbHelper = PFASQLiteHelper.getInstance(context.getApplicationContext());
        int zonemilliseconds = dbHelper.getCurrentWeatherByCityId(cityId).getTimeZoneSeconds() * 1000;
        Log.d("devtag", "zonehours " + zonemilliseconds / 3600000.0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.DST_OFFSET, 0);
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.ZONE_OFFSET, zonemilliseconds);


        long startOfDay = cal.getTimeInMillis();
        Log.d("devtag", "calendar " + cal.getTimeInMillis() + cal.getTime());

        if (System.currentTimeMillis() < startOfDay) cal.add(Calendar.HOUR_OF_DAY, -24);
        if (System.currentTimeMillis() > startOfDay + 24 * 3600 * 1000)
            cal.add(Calendar.HOUR_OF_DAY, 24);
        Log.d("devtag", "calendar " + cal.getTimeInMillis() + cal.getTime());

        //temp max 0, temp min 1, humidity max 2, humidity min 3, wind max 4, wind min 5, wind direction 6, rain total 7, time 8, weather ID 9, number of FCs for day 10
        float[] today = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> todayIDs = new LinkedList<>();
        float[] tomorrow = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> tomorrowIDs = new LinkedList<>();
        float[] in2days = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> in2daysIDs = new LinkedList<>();
        float[] in3days = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> in3daysIDs = new LinkedList<>();
        float[] in4days = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> in4daysIDs = new LinkedList<>();
        float[] in5days = {-Float.MAX_VALUE, Float.MAX_VALUE, 0, 100, 0, Float.MAX_VALUE, 0, 0, Float.MAX_VALUE, 0, 0};
        LinkedList<Integer> in5daysIDs = new LinkedList<>();

        long daystart = cal.getTimeInMillis();
        //iterate over FCs from today and after
        for (Forecast fc : forecastList) {
            long forecastTime = fc.getForecastTime();
            if (fc.getForecastTime() > daystart) {
                //inside current day
                if (forecastTime <= daystart + 86400000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > today[0]) today[0] = fc.getTemperature();
                    if (fc.getTemperature() < today[1]) today[1] = fc.getTemperature();

                    if (fc.getHumidity() > today[2]) today[2] = fc.getHumidity();
                    if (fc.getHumidity() < today[3]) today[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > today[4]) today[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < today[5]) today[5] = fc.getWindSpeed();


                    today[6] += fc.getWindDirection();
                    today[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < today[8]) today[8] = fc.getForecastTime();
                    //count number of FCs
                    today[10] += 1;

                    //count weather id occurrences -> use most common
                    todayIDs.add(fc.getWeatherID());

                    //inside next day...
                } else if (forecastTime <= daystart + 172800000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > tomorrow[0]) tomorrow[0] = fc.getTemperature();
                    if (fc.getTemperature() < tomorrow[1]) tomorrow[1] = fc.getTemperature();

                    if (fc.getHumidity() > tomorrow[2]) tomorrow[2] = fc.getHumidity();
                    if (fc.getHumidity() < tomorrow[3]) tomorrow[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > tomorrow[4]) tomorrow[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < tomorrow[5]) tomorrow[5] = fc.getWindSpeed();

                    tomorrow[6] += fc.getWindDirection();
                    tomorrow[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < tomorrow[8]) tomorrow[8] = fc.getForecastTime();
                    //count number of FCs
                    ++tomorrow[10];

                    //count weather id occurrences -> use most common
                    tomorrowIDs.add(fc.getWeatherID());

                } else if (forecastTime <= daystart + 259200000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > in2days[0]) in2days[0] = fc.getTemperature();
                    if (fc.getTemperature() < in2days[1]) in2days[1] = fc.getTemperature();

                    if (fc.getHumidity() > in2days[2]) in2days[2] = fc.getHumidity();
                    if (fc.getHumidity() < in2days[3]) in2days[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > in2days[4]) in2days[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < in2days[5]) in2days[5] = fc.getWindSpeed();

                    in2days[6] += fc.getWindDirection();
                    in2days[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < in2days[8]) in2days[8] = fc.getForecastTime();
                    //count number of FCs
                    ++in2days[10];

                    //count weather id occurrences -> use most common
                    in2daysIDs.add(fc.getWeatherID());

                } else if (forecastTime <= daystart + 345600000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > in3days[0]) in3days[0] = fc.getTemperature();
                    if (fc.getTemperature() < in3days[1]) in3days[1] = fc.getTemperature();

                    if (fc.getHumidity() > in3days[2]) in3days[2] = fc.getHumidity();
                    if (fc.getHumidity() < in3days[3]) in3days[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > in3days[4]) in3days[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < in3days[5]) in3days[5] = fc.getWindSpeed();


                    in3days[6] += fc.getWindDirection();
                    in3days[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < in3days[8]) in3days[8] = fc.getForecastTime();
                    //count number of FCs
                    ++in3days[10];

                    //count weather id occurrences -> use most common
                    in3daysIDs.add(fc.getWeatherID());

                } else if (forecastTime <= daystart + 432000000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > in4days[0]) in4days[0] = fc.getTemperature();
                    if (fc.getTemperature() < in4days[1]) in4days[1] = fc.getTemperature();

                    if (fc.getHumidity() > in4days[2]) in4days[2] = fc.getHumidity();
                    if (fc.getHumidity() < in4days[3]) in4days[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > in4days[4]) in4days[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < in4days[5]) in4days[5] = fc.getWindSpeed();


                    in4days[6] += fc.getWindDirection();
                    in4days[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < in4days[8]) in4days[8] = fc.getForecastTime();
                    //count number of FCs
                    ++in4days[10];

                    //count weather id occurrences -> use most common
                    in4daysIDs.add(fc.getWeatherID());

                } else if (forecastTime <= daystart + 518400000) {
                    //is temp higher lower than current max/min?
                    if (fc.getTemperature() > in5days[0]) in5days[0] = fc.getTemperature();
                    if (fc.getTemperature() < in5days[1]) in5days[1] = fc.getTemperature();

                    if (fc.getHumidity() > in5days[2]) in5days[2] = fc.getHumidity();
                    if (fc.getHumidity() < in5days[3]) in5days[3] = fc.getHumidity();

                    if (fc.getWindSpeed() > in5days[4]) in5days[4] = fc.getWindSpeed();
                    if (fc.getWindSpeed() < in5days[5]) in5days[5] = fc.getWindSpeed();


                    in5days[6] += fc.getWindDirection();
                    in5days[7] += fc.getRainValue();
                    //earliest forecast Time
                    if (fc.getForecastTime() < in5days[8]) in5days[8] = fc.getForecastTime();
                    //count number of FCs
                    ++in5days[10];

                    //count weather id occurrences -> use most common
                    in5daysIDs.add(fc.getWeatherID());

                }
            }
        }
        //select most common weather ID from the day
        today[9] = mostPrevalentWeather(todayIDs);
        tomorrow[9] = mostPrevalentWeather(tomorrowIDs);
        in2days[9] = mostPrevalentWeather(in2daysIDs);
        in3days[9] = mostPrevalentWeather(in3daysIDs);
        in4days[9] = mostPrevalentWeather(in4daysIDs);
        in5days[9] = mostPrevalentWeather(in5daysIDs);

        //normalize wind direction for number of FCs used for that day and add zonetime
        today[6] /= today[10];
        today[8] = today[8] + zonemilliseconds;
        tomorrow[6] /= tomorrow[10];
        tomorrow[8] = tomorrow[8] + zonemilliseconds;
        in2days[6] /= in2days[10];
        in2days[8] = in2days[8] + zonemilliseconds;
        in3days[6] /= in3days[10];
        in3days[8] = in3days[8] + zonemilliseconds;
        in4days[6] /= in4days[10];
        in4days[8] = in4days[8] + zonemilliseconds;
        in5days[6] /= in5days[10];
        in5days[8] = in5days[8] + zonemilliseconds;
        Log.d("devtag", "total :" + forecastList.size() + "times: " + today[10] + " " + today[8] + " " + tomorrow[10] + " " + tomorrow[8] + " " + in2days[10] + " " + in2days[8] + " " + in3days[10] + " " + in3days[8] + " " + in4days[10] + " " + in4days[8] + " " + in5days[10] + " " + in5days[8]);
        return new float[][]{today, tomorrow, in2days, in3days, in4days, in5days};
    }

    //return most common weather ID from linked list
    private int mostPrevalentWeather(LinkedList<Integer> IDs) {
        int[] counts = {0, 0, 0, 0, 0, 0, 0, 0, 0};

        //count 1 up for every ID in its category
        for (int id : IDs) {
            switch (id) {
                case 10:
                    counts[0] += 1;
                    break;
                case 20:
                    counts[1] += 1;
                    break;
                case 30:
                    counts[2] += 1;
                    break;
                case 40:
                    counts[3] += 1;
                    break;
                case 50:
                    counts[4] += 1;
                    break;
                case 60:
                    counts[5] += 1;
                    break;
                case 70:
                    counts[6] += 1;
                    break;
                case 80:
                    counts[7] += 1;
                    break;
                case 90:
                    counts[8] += 1;
                    break;
            }
        }
        int max = 0;
        int index = 0;
        //search for max count and select max as index
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > max) {
                index = i;
                max = counts[i];
            }
        }
        //weather ID is between 10 and 90
        return (index + 1) * 10;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    public class OverViewHolder extends ViewHolder {
        TextView temperature;
        ImageView weather;

        OverViewHolder(View v) {
            super(v);
            this.temperature = v.findViewById(R.id.activity_city_weather_temperature);
            this.weather = v.findViewById(R.id.activity_city_weather_image_view);
        }
    }

    public class DetailViewHolder extends ViewHolder {
        TextView humidity;
        TextView pressure;
        TextView windspeed;

        DetailViewHolder(View v) {
            super(v);
            this.humidity = v.findViewById(R.id.activity_city_weather_tv_humidity_value);
            this.pressure = v.findViewById(R.id.activity_city_weather_tv_pressure_value);
            this.windspeed = v.findViewById(R.id.activity_city_weather_tv_wind_speed_value);
        }
    }

    public class WeekViewHolder extends ViewHolder {
        RecyclerView recyclerView;

        WeekViewHolder(View v) {
            super(v);
            recyclerView = v.findViewById(R.id.recycler_view_week);
            recyclerView.setHasFixedSize(true);
        }
    }

    public class DayViewHolder extends ViewHolder {
        RecyclerView recyclerView;

        DayViewHolder(View v) {
            super(v);
            recyclerView = v.findViewById(R.id.recycler_view_course_day);
            recyclerView.setHasFixedSize(true);
        }
    }

    public class SunViewHolder extends ViewHolder {
        TextView sunrise;
        TextView sunset;

        SunViewHolder(View v) {
            super(v);
            this.sunrise = v.findViewById(R.id.activity_city_weather_tv_sunrise_value);
            this.sunset = v.findViewById(R.id.activity_city_weather_tv_sunset_value);
        }
    }

    public class ErrorViewHolder extends ViewHolder {
        ErrorViewHolder(View v) {
            super(v);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        if (viewType == OVERVIEW) {
            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_overview, viewGroup, false);

            return new OverViewHolder(v);

        } else if (viewType == DETAILS) {

            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_details, viewGroup, false);
            return new DetailViewHolder(v);

        } else if (viewType == WEEK) {

            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_week, viewGroup, false);
            return new WeekViewHolder(v);

        } else if (viewType == DAY) {

            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_day, viewGroup, false);
            return new DayViewHolder(v);

        } else if (viewType == SUN) {

            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_sun, viewGroup, false);
            return new SunViewHolder(v);
        } else {
            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.card_error, viewGroup, false);
            return new ErrorViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        boolean isDay = currentWeatherDataList.getTimestamp() >currentWeatherDataList.getTimeSunrise() && currentWeatherDataList.getTimestamp() < currentWeatherDataList.getTimeSunset();

        if (viewHolder.getItemViewType() == OVERVIEW) {
            OverViewHolder holder = (OverViewHolder) viewHolder;
            setImage(currentWeatherDataList.getWeatherID(), holder.weather, isDay);

            holder.temperature.setText(StringFormatUtils.formatTemperature(context, currentWeatherDataList.getTemperatureCurrent()));

        } else if (viewHolder.getItemViewType() == DETAILS) {

            DetailViewHolder holder = (DetailViewHolder) viewHolder;
            holder.humidity.setText(StringFormatUtils.formatInt(currentWeatherDataList.getHumidity(), "%"));
            holder.pressure.setText(StringFormatUtils.formatDecimal(currentWeatherDataList.getPressure(), " hPa"));
            holder.windspeed.setText(StringFormatUtils.formatWindSpeed(context, currentWeatherDataList.getWindSpeed()) + " " + StringFormatUtils.formatWindDir(context, currentWeatherDataList.getWindDirection()));

        } else if (viewHolder.getItemViewType() == WEEK) {

            WeekViewHolder holder = (WeekViewHolder) viewHolder;
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            holder.recyclerView.setLayoutManager(layoutManager);
            WeekWeatherAdapter adapter = new WeekWeatherAdapter(forecastData, context);
            holder.recyclerView.setAdapter(adapter);

        } else if (viewHolder.getItemViewType() == DAY) {

            DayViewHolder holder = (DayViewHolder) viewHolder;
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.recyclerView.setLayoutManager(layoutManager);
            CourseOfDayAdapter adapter = new CourseOfDayAdapter(courseDayList, context);
            holder.recyclerView.setAdapter(adapter);

        } else if (viewHolder.getItemViewType() == SUN) {
            SunViewHolder holder = (SunViewHolder) viewHolder;

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            //correct for timezone differences
            int zoneseconds = currentWeatherDataList.getTimeZoneSeconds();
            Date riseTime = new Date((currentWeatherDataList.getTimeSunrise() + zoneseconds) * 1000L);
            Date setTime = new Date((currentWeatherDataList.getTimeSunset() + zoneseconds) * 1000L);

            holder.sunrise.setText(timeFormat.format(riseTime));
            holder.sunset.setText(timeFormat.format(setTime));
        }
        //No update for error needed
    }

    public void setImage(int value, ImageView imageView, boolean isDay) {
        imageView.setImageResource(UiResourceProvider.getImageResourceForWeatherCategory(value, isDay));
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        return dataSetTypes[position];
    }
}