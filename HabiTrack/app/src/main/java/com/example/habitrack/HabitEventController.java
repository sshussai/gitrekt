package com.example.habitrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Calendar;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;

/**
 * HabitEventController
 *
 * Version 1.0
 *
 * Created by sshussai on 10/21/17.
 */

public class HabitEventController {
    /**
     * This class is the main interface for the habit event entity. It can create a new habit event
     * and delete it, given a corresponding habit type
     *
     */

    private Context hectx;
    private final String FILE_NAME = "habitEvents.sav";
    private final String ID_FILE_NAME = "heid.sav";
    private FileManager fileManager;

    public HabitEventController(Context ctx){
        this.hectx = ctx;
        this.fileManager = new FileManager(ctx);
    }

    public void createNewHabitEvent(String esID, Integer habitTypeID, Boolean isConnected, String userID){
        Log.d("seen","itcame here");
        HabitTypeController htc = new HabitTypeController(hectx);
        HabitEvent he = new
                HabitEvent(HabitEventStateManager.getHEStateManager().getHabitEventID(), habitTypeID, esID);
        // Save the new HE ID
        saveHEID();
        he.setTitle(htc.getHabitTitle(esID));
        // set user ID
        he.setUserID(userID);
        // Save event on elastic search if connected
        if(isConnected) {
            ElasticSearchController.AddHabitEvent addHabitEvent = new ElasticSearchController.AddHabitEvent(fileManager);
            addHabitEvent.execute(he);
            // set the current event to be the most recent event for the ht
            htc.setHabitTypeMostRecentEvent(esID, he);
            // Increment the completed event counter for the habit type
            htc.incrementHTMaxCounter(esID);
        } else {
            HabitEventStateManager.getHEStateManager().addNewOfflineHE(he);
        }
        // Set the htmd scheduled today to TRUE
        HabitTypeMetadata htmd = HabitTypeStateManager.getHTStateManager().getHtMetadata(esID);
        htmd.setScheduledToday(Boolean.TRUE);
        fileManager.save(fileManager.HT_METADATA_MODE);
        // add to today's events & save event as part of today's events
        HabitEventStateManager.getHEStateManager().addTodayHabitEvent(he);
        fileManager.save(fileManager.TODAY_HE_MODE);
        // add to recent events & save event as recent event
        HabitEventStateManager.getHEStateManager().addRecentEvent(he);
        fileManager.save(fileManager.RECENT_HE_MODE);
    }

    public ArrayList<HabitEvent> getHabitEventsForToday(){
        return HabitEventStateManager.getHEStateManager().getTodayHabitevents();
    }

    public ArrayList<HabitEvent> getAllHabitEvent(){
        return HabitEventStateManager.getHEStateManager().getAllHabitEvents();
    }

    public void updateRecentHabitEvents(){
        HabitEventStateManager.getHEStateManager().updateRecentHabitEvents();
    }

    public void generateEventsForToday(Boolean isConnected, String userID){
        ArrayList<HabitTypeMetadata> htmdList = HabitTypeStateManager.getHTStateManager().getHtMetadataAll();
        Integer today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        for(HabitTypeMetadata htmd : htmdList){
            ArrayList<Integer> schedule = htmd.getSchedule();
            if(schedule.contains(today)){
                String esID = htmd.getEsID();
                Integer htID = htmd.getLocalID();
                createNewHabitEvent(esID, htID, isConnected, userID);
            }
        }
    }

    /**
     * This function returns the list of recent events
     * @return
     */
    public ArrayList<HabitEvent> getRecentHabitEvents(){
        return HabitEventStateManager.getHEStateManager().getRecentHabitevents();
    }

    public void updateHabitEvents(Boolean isConnected, String userID){
        ArrayList<HabitTypeMetadata> htmdList = HabitTypeStateManager.getHTStateManager().getHtMetadataToday();
        for(HabitTypeMetadata htmd : htmdList){
            String esID = htmd.getEsID();
            if(htmd.getScheduledToday() == Boolean.FALSE && esID != null){
                createNewHabitEvent(esID, htmd.getLocalID(), isConnected, userID);
                htmd.setScheduledToday(Boolean.TRUE);
            }
        }
        fileManager.save(fileManager.HT_METADATA_MODE);
    }


    /**
     * This function deletes all habit events
     */
    public void deleteAllHabitTypes(){
        HabitEventStateManager.getHEStateManager().removeRecentHabitEvents();
        HabitEventStateManager.getHEStateManager().removeAllHabitEvents();
        // Save event locally
        saveToFile();
    }

    /**
     * this function deletes all the habit types scheduled for today
     */
    public void deleteHabitTypesForToday(){
        HabitTypeStateManager.getHTStateManager().removeHabitTypesForToday();
    }

    public void deleteHabitEvent(Integer requestedID) {
        HabitEventStateManager.getHEStateManager().removeHabitEvent(requestedID);
        // Save event locally
        saveToFile();
    }

    public HabitEvent getHabitEvent(Integer requestedID) {
        HabitEvent he = HabitEventStateManager.getHEStateManager().getHabitEvent(requestedID);
        return he;
    }

    public ArrayList<HabitEvent> getHabitEventElasticSearch(){

        ArrayList<HabitEvent> he = new ArrayList<>();
        ElasticSearchController.GetHabitEvent getHabitEvent = new ElasticSearchController.GetHabitEvent();
        getHabitEvent.execute("");
        try {
            he = getHabitEvent.get();
        }
        catch (Exception e)
        {
            Log.i("Error","Failed to get the tweets from the async object");
        }

        return he;
    }

    public void editHabitEventTitle(Integer requestedID, String newTitle){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setTitle(newTitle);
            // Save event locally
            saveToFile();
        }
    }

//    public void editHabitEventPhoto(Integer requestedID, Bitmap newPhoto){
//        HabitEvent he = this.getHabitEvent(requestedID);
//        // If the habit event exists
//        if(!he.getHabitEventID().equals(-1)){
//            he.setPhoto(newPhoto);
//            // Save event locally
//            saveToFile();
//        }
//    }

    public void editHabitEventComment(Integer requestedID, String newComment, Boolean isConnected){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setComment(newComment);
            // Save event locally
            saveToFile();
            if(isConnected){
                ElasticSearchController.EditHabitEvent editHabitEvent = new ElasticSearchController.EditHabitEvent(fileManager);
                editHabitEvent.execute(he);
            } else {
                HabitEventStateManager.getHEStateManager().addEditedOfflineHE(he);
            }
        }
    }

    public void syncEditedHabitEvents(){
        ArrayList<HabitEvent> editedHEs = HabitEventStateManager.getHEStateManager().getEditedOfflineHE();
        if(editedHEs.size() > 0 ) {
            ElasticSearchController.syncEditedOfflineHEs syncer = new ElasticSearchController.syncEditedOfflineHEs(fileManager);
            syncer.execute();
        }
    }

    public void syncNewOfflineHEs(){
        ArrayList<HabitEvent> newOfflineHEs = HabitEventStateManager.getHEStateManager().getNewOfflineHE();
        if(newOfflineHEs.size() > 0){
            HabitTypeController htc = new HabitTypeController(hectx);
            while(newOfflineHEs.size() > 0) {
                HabitEvent habitEvent = newOfflineHEs.get(0);
                Boolean result = Boolean.FALSE;
                ElasticSearchController.AddHabitEvent addHabitEvent = new ElasticSearchController.AddHabitEvent(fileManager);
                addHabitEvent.execute(habitEvent);
                try {
                    result = addHabitEvent.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if(result){
                    String htEsID = habitEvent.getHabitTypeEsID();
                    htc.incrementHTMaxCounter(htEsID);
                    newOfflineHEs.remove(habitEvent);
                }
            }
        }
    }

    public void syncCompletedOfflineHEs(){
        ArrayList<HabitEvent> newOfflineHEs = HabitEventStateManager.getHEStateManager().getCompletedOfflineHE();
        if(newOfflineHEs.size() > 0){
            HabitTypeController htc = new HabitTypeController(hectx);
            while(newOfflineHEs.size() > 0) {
                HabitEvent habitEvent = newOfflineHEs.get(0);
                Boolean result = Boolean.FALSE;
                ElasticSearchController.EditHabitEvent editHabitEvent = new ElasticSearchController.EditHabitEvent(fileManager);
                editHabitEvent.execute(habitEvent);
                try {
                    result = editHabitEvent.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if(result){
                    String htEsID = habitEvent.getHabitTypeEsID();
                    htc.incrementHTCurrentCounter(htEsID);
                    newOfflineHEs.remove(habitEvent);
                }
            }
        }
    }

    public void editHabitEventDate(Integer requestedID, Calendar newDate){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setDate(newDate);
            // Save event locally
            saveToFile();
        }
    }

    public String getHabitEventTitle(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getTitle();
        } else {
            return "";
        }
    }

    public String getHabitEventComment(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getComment();
        } else {
            return "";
        }
    }

    public Calendar getHabitEventDate(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        Calendar cal;
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            cal = he.getDate();
        } else {
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, -1);
        }
        return cal;
    }

    public void setHabitEventLocation(Integer requestedID, LatLng loc){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setLocation(loc);
            fileManager.save(fileManager.TODAY_HE_MODE);
            HabitEventStateManager.getHEStateManager().addEditedOfflineHE(he);
        }
    }

    public LatLng getHabitEventLocation(Integer requestedID){
        LatLng ret = null;
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getLocation();
        } else {
            return ret;
        }
    }


    public Integer getHabitEventID(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getHabitEventID();
        } else {
            return -1;
        }
    }

    public Integer getCorrespondingHabitTypeID(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getHabitTypeID();
        } else {
            return -1;
        }
    }


    public void setHabitEventEncodedPhoto(Integer requestedID, String encodedPhoto){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setEncodedPhoto(encodedPhoto);
            HabitEventStateManager.getHEStateManager().addEditedOfflineHE(he);
        }
    }

    public String getHabitEventEncodedPhoto(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getEncodedPhoto();
        } else {
            return "";
        }
    }

    public void setHabitEventDecodedPhoto(Integer requestedID, Bitmap photo){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setDecodedPhoto(photo);
        }
    }

    public Bitmap getHabitEventDecodedPhoto(Integer requestedID){
        Bitmap ret = null;
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getDecodedPhoto();
        } else {
            return ret;
        }
    }

    public void completeHabitEvent(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            he.setEmpty(Boolean.FALSE);
        }
        fileManager.save(fileManager.TODAY_HE_MODE);
        fileManager.save(fileManager.RECENT_HE_MODE);
        saveToFile();
    }

    public Boolean getHabitEventIsEmpty(Integer requestedID){
        HabitEvent he = this.getHabitEvent(requestedID);
        // If the habit event exists
        if(!he.getHabitEventID().equals(-1)){
            return he.getEmpty();
        } else {
            return Boolean.TRUE;
        }
    }

    public void loadFromFile() {
        ArrayList<HabitEvent> heList = new ArrayList<HabitEvent>();
        try {
            FileInputStream fis = hectx.openFileInput(FILE_NAME);
            BufferedReader in = new BufferedReader(new InputStreamReader(fis));
            Gson gson = new Gson();
            //Code taken from http://stackoverflow.com/questions/12384064/gson-convert-from-json-to-a-typed-arraylistt Sept.22,2016
            Type listType = new TypeToken<ArrayList<HabitEvent>>(){}.getType();
            heList = gson.fromJson(in, listType);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            heList = new ArrayList<HabitEvent>();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }

        ArrayList<HabitEvent> heForToday = new ArrayList<HabitEvent>();
        Calendar today = Calendar.getInstance();
        for(HabitEvent he : heList){
            Calendar heDate = he.getDate();
            if(heDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && heDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && heDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)){
                heForToday.add(he);
            }
        }
        HabitEventStateManager.getHEStateManager().setAllHabitEventsForToday(heForToday);
        HabitEventStateManager.getHEStateManager().setAllHabitEvents(heList);
    }

    public void saveToFile() {
        ArrayList<HabitEvent> heList = getAllHabitEvent();
        try {
            FileOutputStream fos = hectx.openFileOutput(FILE_NAME,0);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            Gson gson = new Gson();
            gson.toJson(heList, writer);
            writer.flush();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }
    }

    public void saveHEID(){
        Integer saveID = HabitEventStateManager.getHEStateManager().getIDToSave();

        try {
            FileOutputStream fos = hectx.openFileOutput(ID_FILE_NAME,0);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            Gson gson = new Gson();
            gson.toJson(saveID, writer);
            writer.flush();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }
    }

    public void loadHEID() {
        Integer loadedID;
        try {
            FileInputStream fis = hectx.openFileInput(ID_FILE_NAME);
            BufferedReader in = new BufferedReader(new InputStreamReader(fis));
            Gson gson = new Gson();
            //Code taken from http://stackoverflow.com/questions/12384064/gson-convert-from-json-to-a-typed-arraylistt Sept.22,2016
            Type intType = new TypeToken<Integer>(){}.getType();
            loadedID = gson.fromJson(in, intType);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            loadedID = 0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }
        HabitEventStateManager.getHEStateManager().setID(loadedID);
    }


}
