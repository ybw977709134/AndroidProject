package co.onemeter.oneapp.utils;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import co.onemeter.oneapp.ui.TimePiece;
import co.onemeter.utils.AsyncTaskExecutor;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.Log;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WowTalkWebServerIF;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 4/10/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class LocationHelper {
    private static final int TIME_LIMIT = 10;

    private Context mContext;

    private LocationManager locationManager;

    private Location mLocation;

    private String mStrAddress="";

    private OnLocationGotListener mOnLocationGotListener;
    private OnLocationUselessListener mOnLocationUselessListener;

    private TimePiece timePiece;

    private boolean isWithAddress;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            stop();
//            timePiece.stop();
            Log.i("onLocationChanged ... and mLocation is null : " + mLocation == null);
            mLocation = location;
//            locationManager.removeUpdates(locationListener);
            if (isWithAddress) {
                getAddress();
            } else {
                notifyMyLocation();
            }
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onProviderEnabled(String provider) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onProviderDisabled(String provider) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    public LocationHelper(Context context) {
        mContext = context;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        timePiece = new TimePiece();
    }

    private boolean isGPSAvailable() {
        if (locationManager == null) {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isNetAvailable() {
        if (locationManager == null) {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void getGPSLocation() {
        Log.i("request update location with GPS");
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Log.i("best provider:"+provider);
        locationManager.requestLocationUpdates(provider, 1000, 0, locationListener);
    }

    private void getNetWorkLocation() {
        Log.i("request update location with NETWORK");
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
    }

    private boolean getLastKnownGPSLocation() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (mLocation != null)
            return true;
        return false;
    }

    private boolean getLastKnownNetWorkLocation() {
        mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (mLocation != null)
            return true;
        return false;
    }

    private AsyncTask<Void, Void, String> getAddressTask;
    private void getAddress() {
    	if(getAddressTask == null || getAddressTask.getStatus() == AsyncTask.Status.FINISHED)
    	{
    		getAddressTask = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    URL myFileUrl = null;
                    String jsonStr = null;
                    String addressStr = "";
                    try {
                        myFileUrl = new URL("http://maps.google.com/maps/api/geocode/json?latlng=" +
                                mLocation.getLatitude() + "," + mLocation.getLongitude() + "&sensor=true&language=" + Locale.getDefault().getLanguage());
                    }
                    catch (MalformedURLException e) {
                        e.printStackTrace();
                        return null;
                    }
                    HttpURLConnection httpURLConnection = null;
                    InputStream in = null;
                    try {
                        httpURLConnection = (HttpURLConnection) myFileUrl.openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setConnectTimeout(5000);
                        httpURLConnection.setReadTimeout(5000);
                        httpURLConnection.connect();
                        in = httpURLConnection.getInputStream();
                        StringBuffer out = new StringBuffer();
                        byte[] bytes = new byte[4096];
                        int n;
                        while ((n = in.read(bytes)) != -1) {
                            out.append(new String(bytes, 0, n));
                            jsonStr = out.toString();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    finally {
                        if (in != null)
                            try {
                                in.close();
                            }
                            catch (IOException e) {
                            }
                        if (httpURLConnection != null)
                            httpURLConnection.disconnect();
                    }
                    try {
                        JSONObject json = new JSONObject(jsonStr);
                        JSONArray jsonArray = json.getJSONArray("results");
                        JSONObject addressObject = jsonArray.getJSONObject(0);
                        if (addressObject != null && addressObject.has("formatted_address")) {
                            addressStr = addressObject.getString("formatted_address");
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return addressStr;
                }

                @Override
                protected void onPostExecute(String result) {
                    mStrAddress = result;
                    notifyMyLocation();
                    getAddressTask = null;
                }
            };
    	}
    }

    /**
     * 根据经纬度获取未知详情
     * @param context
     * @param latitude
     * @param longtiude
     * @param textView
     */
    public static void fetchAddressFromLatitudeAndLongitude(Context context,double latitude,double longtiude,final TextView textView){
    	LatLonPoint point = new LatLonPoint(latitude, longtiude);
    	RegeocodeQuery query = new RegeocodeQuery(point, 200,GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
    	GeocodeSearch geocoderSearch = new GeocodeSearch(context);
		geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
		OnGeocodeSearchListener onGeocodeSearchListener = new OnGeocodeSearchListener() {
			
			@Override
			public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
				//android.util.Log.i("AMap Code", rCode + "");
				if (rCode == 0) {
					if (result != null && result.getRegeocodeAddress() != null
							&& result.getRegeocodeAddress().getFormatAddress() != null) {
						String addressName = result.getRegeocodeAddress().getFormatAddress();
						textView.setText(addressName);
					}
				} 
			}
			
			@Override
			public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
				
			}
		};
		geocoderSearch.setOnGeocodeSearchListener(onGeocodeSearchListener);
    }
    
    
    public static void addressForMessageComposer(final Context context,final ChatMessage chatMessage,final double latitude,final double longtiude){
    	LatLonPoint point = new LatLonPoint(latitude, longtiude);
    	RegeocodeQuery query = new RegeocodeQuery(point, 200,GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
    	GeocodeSearch geocoderSearch = new GeocodeSearch(context);
		geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
		OnGeocodeSearchListener onGeocodeSearchListener = new OnGeocodeSearchListener() {
			
			@Override
			public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
				//android.util.Log.i("AMap Code", rCode + "");
				if (rCode == 0) {
					if (result != null && result.getRegeocodeAddress() != null
							&& result.getRegeocodeAddress().getFormatAddress() != null) {
						String addr = result.getRegeocodeAddress().getFormatAddress();
		                JSONObject json=new JSONObject();
		                try {
		                    json.put("longitude", longtiude);
		                    json.put("latitude", latitude);
		                    json.put("address", addr);
		                } catch (JSONException e) {
		                }
		                chatMessage.messageContent = json.toString();

		                new Database(context).updateChatMessage(chatMessage, true);
		                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>(){

							@Override
							protected Void doInBackground(Void... params) {
								if(chatMessage.isGroupChatMessage) {
									WowTalkWebServerIF.getInstance(context).fGroupChat_SendMessage(chatMessage.chatUserName, chatMessage);
				                } else {
				                    WowTalkVoipIF.getInstance(context).fSendChatMessage(chatMessage);
				                }
								return null;
							}
		                	
		                });
					}
				} 
			}
			
			@Override
			public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
				
			}
		};
		geocoderSearch.setOnGeocodeSearchListener(onGeocodeSearchListener);
    }
    
    /*
    public static String getAddressFromLatitudeAndLongitude(final double latitude,final double longitude,final TextView textView){
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                URL myFileUrl = null;
                String jsonStr = null;
                String addressStr = "";
                try {
                    myFileUrl = new URL("http://maps.google.com/maps/api/geocode/json?latlng=" +
                    		latitude + "," + longitude + "&sensor=true&language="+ Locale.getDefault().getLanguage());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
                HttpURLConnection httpURLConnection = null;
                InputStream in = null;
                try {
                    httpURLConnection = (HttpURLConnection) myFileUrl.openConnection();
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.setReadTimeout(5000);
                    httpURLConnection.connect();
                    in = httpURLConnection.getInputStream();
                    StringBuffer out = new StringBuffer();
                    byte[] bytes = new byte[4096];
                    int n;
                    while ((n = in.read(bytes)) != -1) {
                        out.append(new String(bytes, 0, n));
                        jsonStr = out.toString();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                finally
                {
                	if(in != null)
						try {
							in.close();
						} catch (IOException e) {
						}
                	if(httpURLConnection !=null)
                		httpURLConnection.disconnect();
                }
                try {
                    JSONObject json = new JSONObject(jsonStr);
                    JSONArray jsonArray = json.getJSONArray("results");
                    JSONObject addressObject = jsonArray.getJSONObject(0);
                    if (addressObject != null && addressObject.has("formatted_address")) {
                        addressStr = addressObject.getString("formatted_address");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
                return addressStr;
            }

            @Override
            protected void onPostExecute(String result) {
            	textView.setText(result);
            }
        });
    	return null;
    }
    */
    
    private void notifyMyLocation() {
        if (mOnLocationGotListener != null) {
            if (null != mLocation && null != mStrAddress) {
                mOnLocationGotListener.onLocationGot(mLocation, mStrAddress);
            } else {
                mOnLocationGotListener.onNoLocationGot();
            }
        }
    }

    private void triggerLocationUpdate() {
        if(isGPSAvailable()) {
            getGPSLocation();
        } else {
            getNetWorkLocation();
        }
        getLastLocation();
    }

    private void getLastLocation() {
        if(!getLastKnownGPSLocation()) {
            getLastKnownNetWorkLocation();
        }
        if(null != mLocation) {
            Log.i("last known location got");
        }
    }

    private LocationManagerProxy aMapLocationManagerProxy;
    private AMapLocationListener aMapLocationListener=new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            Log.i("ampa location changed: "+aMapLocation.toString());
            mLocation=aMapLocation;
            mStrAddress = aMapLocation.getExtras().getString("desc");
            stop();
            if (null != mLocation && isWithAddress) {
            	notifyMyLocation();
            } 
        }

        @Override
        public void onLocationChanged(Location location) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onProviderEnabled(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onProviderDisabled(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };
    public void getLocationWithAMap(final boolean withAddress) {
        isWithAddress=withAddress;
        if(null == aMapLocationManagerProxy) {
            aMapLocationManagerProxy=LocationManagerProxy.getInstance(mContext);
        }
        aMapLocationManagerProxy.requestLocationUpdates(
                LocationProviderProxy.AMapNetwork, 5000, 10, aMapLocationListener);
        timePiece = new TimePiece();
        timePiece.setOnCountDownFinished(new TimePiece.OnCountDownFinished() {
            @Override
            public void onCountDownFinished() {
                timePiece.stop();
                getLocation(withAddress);
            }
        });
        timePiece.startCountDown(TIME_LIMIT);
    }

    public void getLocation(boolean withAddress) {
        isWithAddress = withAddress;
        getLastLocation();

        if(null == mLocation) {
            timePiece = new TimePiece();
            timePiece.setOnCountDownFinished(new TimePiece.OnCountDownFinished() {
                @Override
                public void onCountDownFinished() {
                    stop();
                    notifyMyLocation();
                }
            });
            timePiece.setOnCountDownChangedListener(new TimePiece.OnCountDownChangedListener() {
                @Override
                public void onCountDownChanged(int countDownSecond) {
                    triggerLocationUpdate();
                }
            });
            timePiece.startCountDown(TIME_LIMIT);
        } else {
            if (mLocation != null && isWithAddress) {
                getAddress();
            } else {
                notifyMyLocation();
            }
        }
    }

    public void stop() {
        Log.i("location helper stop");
        timePiece.stop();
        locationManager.removeUpdates(locationListener);

        if (null != aMapLocationManagerProxy) {
            aMapLocationManagerProxy.removeUpdates(aMapLocationListener);
            aMapLocationManagerProxy.destory();
        }
        aMapLocationManagerProxy = null;
    }

    public void setOnLocationGotListener(OnLocationGotListener listener) {
        mOnLocationGotListener = listener;
    }

    public void setOnLocationUselessListener(OnLocationUselessListener listener) {
        mOnLocationUselessListener = listener;
    }

    public interface OnLocationGotListener {
        public void onLocationGot(Location location, String strAddress);
        public void onNoLocationGot();
    }

    public interface OnLocationUselessListener {
        public void onLocationUseless();
    }
}
