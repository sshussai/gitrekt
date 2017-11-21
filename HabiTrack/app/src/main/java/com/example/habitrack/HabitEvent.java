package com.example.habitrack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import io.searchbox.annotations.JestId;


/**
 * HabitEvent
 *
 * Version 1.0
 *
 * Created by sshussai on 10/21/17.
 *
 */



public class HabitEvent {
    /**
     * Version 1.0
     * This class is the template for a habit event entity.
     *
     */

    private final Integer habitEventID;         // ID for the event
    private final Integer habitTypeID;          // ID for the corresponding habit type
    private String title;                       // title of the habit event
    private String comment;                     // comment for the habit event
    private Calendar date;                      // date of the habit event
    @JestId
    private String id;
    private String encodedPhoto;


    public HabitEvent(Integer heID, Integer htID) {
        this.habitEventID = heID;
        this.habitTypeID = htID;
        this.date = Calendar.getInstance();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public Integer getHabitEventID() {
        return habitEventID;
    }

    public Integer getHabitTypeID() {
        return habitTypeID;
    }

    public String getPhoto() {
        return this.encodedPhoto;
    }

    public void setPhoto(String newEncodedPhoto) {
        this.encodedPhoto = newEncodedPhoto;
    }

    public Boolean searchInComment(String key){
        Boolean retVal = Boolean.FALSE;
        if(this.comment.contains(key)){
            retVal = Boolean.TRUE;
        }
        return retVal;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return (getTitle() + "\n" + getComment());
    }
}
