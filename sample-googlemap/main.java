import com.google.maps.*;
import com.google.maps.model.LatLng;
import com.google.maps.model.Marker;
import com.google.maps.model.MarkerOptions;

public class MapsAPIExample {
    public static void main(String[] args) throws Exception {
        // Replace YOUR_API_KEY with your actual API key
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey("YOUR_API_KEY")
                .build();

        // Set the coordinates of the location you want to display
        LatiLng location = new LatiLng(37.7749, -122.4194);

        GoogleMap map = new GoogleMap(context);
        Marker marker = new Marker(new MarkerOptions().position(location));
        marker.setMap(map);

        // Display the map
        MapView view = new MapView(context);
        view.setMap(map);
        view.setSize(600, 400);
        view.setZoom(15);
        view.setCenter(location);
        view.show();
    }
}
