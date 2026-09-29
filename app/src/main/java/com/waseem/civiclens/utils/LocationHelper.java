package com.waseem.civiclens.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface AddressCallback {
        void onAddressFound(String address);
    }

    public static void getAddressFromCoordinates(Context context, double lat, double lng, AddressCallback callback) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (address.getMaxAddressLineIndex() >= 0) {
                        sb.append(address.getAddressLine(0));
                    } else {
                        if (address.getFeatureName() != null) sb.append(address.getFeatureName()).append(", ");
                        if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                        if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                        if (address.getAdminArea() != null) sb.append(address.getAdminArea());
                    }
                    String finalAddress = sb.toString().trim();
                    if (finalAddress.endsWith(",")) finalAddress = finalAddress.substring(0, finalAddress.length() - 1);
                    String res = finalAddress;
                    callback.onAddressFound(res);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            callback.onAddressFound(String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng));
        }).start();
    }
}
