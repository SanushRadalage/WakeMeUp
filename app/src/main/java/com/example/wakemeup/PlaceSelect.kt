package com.example.wakemeup

import android.app.DownloadManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ListView
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_place_select.*
import retrofit2.Retrofit

//import android.support.v7.app.AppCompatActivity



class PlaceSelect : AppCompatActivity() {

    lateinit var jsonUrl:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_select)

        enter_place.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jsonUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input="+s+"&types=geocode&location=6.927079,79.861244&radius=2000&key=AIzaSyAG36QakYK2Q7Ma6bQlal4we7Vv6fKuks8"
                run(jsonUrl)
            }
        })
    }

    fun run(url: String){
//        tvSample.setText(url)
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .build()


    }

}
