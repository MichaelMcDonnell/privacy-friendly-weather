package org.secuso.privacyfriendlyweather.ui;

import org.secuso.privacyfriendlyweather.orm.CityToWatch;
import org.secuso.privacyfriendlyweather.orm.DatabaseHelper;
import org.secuso.privacyfriendlyweather.weather_api.IHttpRequestForCityList;

import java.sql.SQLException;
import java.util.List;

/**
 * This class provides various methods for updating the UI, e.g. with the latest data.
 */
public class UIUpdater {

    /**
     * Constants.
     */
    private final String DEBUG_TAG = "ui_updated_debug";

    /**
     * Member variables
     */
    private DatabaseHelper dbHelper;

    /**
     * Constructor.
     *
     * @param dbHelper A DatabaseHelper instance in order to perform database queries.
     */
    public UIUpdater(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * @param apiToUse An implementation of IHttpRequestForCityList which performs the HTTP request
     *                 to the weather API.
     */
    public void updateCitiesList(IHttpRequestForCityList apiToUse) {
        try {
            // Get all the added cities and build the groupID for the HTTP request
            List<CityToWatch> cityToWatches = dbHelper.getCityToWatchDao().queryForAll();
            apiToUse.perform(cityToWatches);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
