// Remove for loops in listeners

package com.awesome.mystartup

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup_layout.*
import kotlinx.android.synthetic.main.popup_layout.view.*
import kotlinx.android.synthetic.main.wait_turn_popup_layout.*
import kotlinx.android.synthetic.main.wait_turn_popup_layout.view.*
import kotlin.Exception


class MainActivity : AppCompatActivity() {
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    private var database = FirebaseDatabase.getInstance()
    private var myRef = database.reference

    private var myEmail:String? = null
    private var myUid:String? = null
    private var userEmail:String?=null
    private var myPlay:String? = null
    private var userPlay:String? = null
    private var userSymbol:String? = ""
    private var mySymbol:String? = ""
    private lateinit var generalWindow:PopupWindow
    private lateinit var window:PopupWindow




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        var bundle:Bundle? = intent.extras
        myEmail = bundle!!.getString("email")
        myUid = bundle!!.getString("uid")

        clearDatabase()
        disable_all()

        myRef.child("users").child(SplitEmail(myEmail!!)).child("request").push().setValue(null)
        myRef.child("users").child(SplitEmail(myEmail!!)).child("symbol").push().setValue(null)
        myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").push().setValue(null)
        myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").push().setValue(null)
        incomingCalls()
//        dismissGeneralPopup()
//        generalPopup("Wanna play a game? You gotta enter email ID and add a friend first.")

        myRef.child("users").child(SplitEmail(myEmail!!)).child("turn")
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    try{
                        var dataMap = p0.value as HashMap<String,Any>

                        if(dataMap != null){
                            var valueSet = dataMap.values
                            var isMyTurn = valueSet.elementAt(0) as Boolean
                            if(true == isMyTurn){
                                updatePlayStrings() // update myPlayString and userPlayString local variables
                                updateTileSymbols(myPlay,mySymbol) // Update symbols
                                updateTileSymbols(userPlay,userSymbol)
                                updateClickableTiles() // Update clickable tiles
                                dismissGeneralPopup()
                                generalPopup("Hey! It's your turn.") //Only shows a pop up, which is dismissed in buClick

                            }
                            else{
                                //If it's not my turn

                                var resultWin = checkWinner(myPlay)
                                if(resultWin){
                                    myRef.child("users").child(SplitEmail(myEmail!!)).child("result").push().setValue(1)
                                    myRef.child("users").child(SplitEmail(userEmail!!)).child("result").push().setValue(-1)
                                }

                                var resultWinUser = checkWinner(userPlay)
                                var resultDraw = false
                                if(resultWinUser){
                                    myRef.child("users").child(SplitEmail(userEmail!!)).child("result").push().setValue(1)
                                    myRef.child("users").child(SplitEmail(myEmail!!)).child("result").push().setValue(-1)
                                }
                                else{
                                    resultDraw = checkDraw(myPlay,userPlay)
                                    if(resultDraw){
                                        myRef.child("users").child(SplitEmail(myEmail!!)).child("result").push().setValue(0)
                                        myRef.child("users").child(SplitEmail(userEmail!!)).child("result").push().setValue(0)
                                    }
                                }

                                if (!(resultWin || resultWinUser || resultDraw)){
                                    //Following won't run if there's a win, loss or draw
                                    disable_all()
                                    dismissGeneralPopup()
                                    generalPopup("Wait! It's not your turn.") // shows a popup telling user to wait

                                }
                            }
                        }

                    }catch (ex:Exception){ }

                }

            })

        // RESULT LISTENER

        myRef.child("users").child(SplitEmail(myEmail!!)).child("result")
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    try{
                        var dataMap = p0.value as HashMap<String,Any>

                        if(dataMap != null){
                            var valueSet = dataMap.values
                            var result = valueSet.elementAt(0) as Long
                            if(1.toLong() == result){
                                // Win
                                dismissGeneralPopup()
                                generalPopup("Congrats! You won!")
                                disable_all()
                            }
                            if(-1.toLong() == result){
                                //Loss
                                dismissGeneralPopup()
                                generalPopup("Better luck next time!")
                                disable_all()
                            }
                            if(0.toLong() == result){
                                //Draw
                                dismissGeneralPopup()
                                generalPopup("It's a draw!!")
                                disable_all()
                            }
                        }

                    }catch (ex:Exception){
                    }

                }

            })

    }


    fun clearDatabase(){
        myRef.child("users").child(SplitEmail(myEmail!!)).child("request").removeValue()
        myRef.child("users").child(SplitEmail(myEmail!!)).child("symbol").removeValue()
        myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").removeValue()
        myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").removeValue()
        myRef.child("users").child(SplitEmail(myEmail!!)).child("play").removeValue()
        myRef.child("users").child(SplitEmail(myEmail!!)).child("result").removeValue()

    }

    fun buRequestEvent(view: View){
        //Sends user requests
        // if I send request, add my email ID to other User's email ID
        dismissGeneralPopup()
        generalPopup("Request sent. Please wait for your friend's response.")
        userEmail = etEmailInput.text.toString()
        myRef.child("users").child(SplitEmail(userEmail!!)).child("request").push().setValue(SplitEmail(myEmail!!))
        myRef.child("users").child(SplitEmail(userEmail!!)).child("requestAccepted").
                addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        try{
                            var dataMap = p0.value as HashMap<String,Any>
                            if(dataMap != null){
                                var valueSet = dataMap.values    //-------------------------------------------IMPORTANT
                                var data = valueSet.elementAt(0) as Boolean
                                if(data == true){
                                    updateSymbols()
                                    updatePlayStrings() // Updates myPlay and userPlay string locally
                                    updateClickableTiles() //Updates clickable tiles
                                    Toast.makeText(this@MainActivity,"Yay! Request accepted",Toast.LENGTH_LONG).show()
                                    myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").push().setValue(true)
                                }
                                else{
                                    Toast.makeText(this@MainActivity,"Request rejected",Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch(ex:kotlin.Exception){}

                    }

                })
    }


    fun buClick (view:View)
    {
        val buSelected = view as ImageButton // Gets you access to the view of element, in this case Button. BUTTON IS THE TYPE OF
        // buSelected in this case

        if(myPlay == null){
            myPlay = ""
        }

        when(buSelected.id)
        {  // PUT UPDATE DATABASE AND MYTURN AT THE END
            R.id.bu1 -> {
                myPlay += "1"
            }
            R.id.bu2 -> {
                myPlay += "2"
            }
            R.id.bu3 -> {
                myPlay += "3"
            }
            R.id.bu4 -> {
                myPlay += "4"
            }
            R.id.bu5 -> {
                myPlay += "5"
            }
            R.id.bu6 -> {
                myPlay += "6"
            }
            R.id.bu7 -> {
                myPlay += "7"
            }
            R.id.bu8 -> {
                myPlay += "8"
            }
            R.id.bu9 -> {
                myPlay += "9"
            }
        }
        updateDatabase() // Updates playStrings to database    BUG: MYTURN AND UPDATE DATABASE DOESN'T GET CALLED IN CASE OF 2ND PLAYER
        updateTileSymbols(myPlay,mySymbol)
        updateTileSymbols(userPlay,userSymbol)
        myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").removeValue() // Remove previous value
        myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").push().setValue(false) // After I click, my turn is over
        myRef.child("users").child(SplitEmail(userEmail!!)).child("turn").removeValue() // Remove Previous value
        myRef.child("users").child(SplitEmail(userEmail!!)).child("turn").push().setValue(true) // User turn started

    }




    fun incomingCalls(){
        //Detects user requests
        // As soon as user 1's request hits user 2's DB, this will get the user 1 details and show it to user2

        myRef.child("users").child(SplitEmail(myEmail!!)).child("request")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {
                    try{
                        val td = p0.value as HashMap<String,Any>

                        if(td != null){
                            var value:String
                            var popupStr:String
                            var valueSet = td.values
                            value = valueSet.elementAt(0) as String
                            userEmail = value

                            try{
                                window.dismiss()
                            }catch (ex:Exception){}


                            val view = layoutInflater.inflate(R.layout.popup_layout,null)
                            popupStr = value+" has sent a request"

                            popupStr = newLineFormatting(popupStr,18)
                            view.tvPopup.text = popupStr
                            window = PopupWindow(this@MainActivity)
                            window.contentView = view
                            window.setBackgroundDrawable(resources.getDrawable(R.color.transparentBlack,resources.newTheme()))
                            window.showAtLocation(view, Gravity.CENTER,0,0)

                            view.ivAccept.setOnClickListener{
                                //Accepts user requests
                                //if I accept request, add my email ID to other user's email ID
                                var user = "x"  // Sender X and receiver O
                                var me = "o"
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("symbol").push().setValue(me)
                                myRef.child("users").child(SplitEmail(userEmail!!)).child("symbol").push().setValue(user)
                                updateSymbols()
                                window.dismiss()
                                updatePlayStrings() // Updates myPlay and userPlay string locally
                                updateClickableTiles() //Updates clickable tiles
                                // myTurn is nothing but a value listener. It should be initiated only when there's other user who accepted/sent request
                                //Above won't make any changes in turns, will just set a listener in ON mode
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("turn").push().setValue(false)
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("request").removeValue()
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").removeValue()
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").push().setValue(true)

                            }

                            view.ivReject.setOnClickListener{
                                // Clearing the shown request, any value okay instad of true
                                //Set null in receiver database
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("request").removeValue()
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").removeValue()
                                myRef.child("users").child(SplitEmail(myEmail!!)).child("requestAccepted").push().setValue(false)
                                window.dismiss()
                            }
                        }
                    }
                    catch (ex:Exception){
//                        etEmailInput.setText(ex.toString())
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })
    }

    fun newLineFormatting(popupStr:String, charLimit:Int):String{
        var tempStrList = popupStr.split(" ")
        var tempStr = ""
        var finalStr = ""
        for (eachElem in tempStrList){
            tempStr += eachElem
            if(tempStr.length >= charLimit){ // 18 is the num of char that can fit in one line of popupWindow
                tempStr += System.getProperty("line.separator")
                finalStr += tempStr
                tempStr = ""
            }
            else{
                tempStr += " "
            }
        }
        finalStr += tempStr
        return finalStr
    }

    fun SplitEmail(email:String):String{
        var splitEmail = email.split("@")
        return splitEmail[0]
    }

    fun dismissGeneralPopup(){
        try{
            generalWindow.dismiss()
        }catch (ex:Exception){}
    }

    fun generalPopup(message:String){
        //Use this function for following: 1.Win 2.Draw 3.Wait 4.YourTurn
        // Popups don't disable buttons
        var generalView = layoutInflater.inflate(R.layout.wait_turn_popup_layout,null)
        generalWindow = PopupWindow(this@MainActivity)
        generalView.tvWait.text = newLineFormatting(message,28)
        generalWindow.contentView = generalView
        generalWindow.setBackgroundDrawable(resources.getDrawable(R.color.transparentBlack,resources.newTheme()))
        generalWindow.showAtLocation(generalView,Gravity.CENTER,0,-500)
    }


    fun updateClickableTiles(){
        // MUST UPDATE LOCAL MYPLAY AND USERPLAY STRINGS BEFORE CALLING THIS FUNCTION

        var totalString = "123456789"
        if((myPlay != null)){
            for (tile in totalString){
                if(!(tile in myPlay.toString())){
                    when(tile){
                        '1'->{bu1.isEnabled = true}
                        '2'->{bu2.isEnabled = true}
                        '3'->{bu3.isEnabled = true}
                        '4'->{bu4.isEnabled = true}
                        '5'->{bu5.isEnabled = true}
                        '6'->{bu6.isEnabled = true}
                        '7'->{bu7.isEnabled = true}
                        '8'->{bu8.isEnabled = true}
                        '9'->{bu9.isEnabled = true}
                    }
                }
            }
        }
        if(userPlay != null){
            for (tile in totalString){
                if(!(tile in userPlay.toString())){
                    when(tile){
                        '1'->{bu1.isEnabled = true}
                        '2'->{bu2.isEnabled = true}
                        '3'->{bu3.isEnabled = true}
                        '4'->{bu4.isEnabled = true}
                        '5'->{bu5.isEnabled = true}
                        '6'->{bu6.isEnabled = true}
                        '7'->{bu7.isEnabled = true}
                        '8'->{bu8.isEnabled = true}
                        '9'->{bu9.isEnabled = true}
                    }
                }
            }
        }

        if((myPlay == null) && (userPlay == null)){
            // if both are null, all buttons should be clickable
                bu1.isEnabled = true
                bu2.isEnabled = true
                bu3.isEnabled = true
                bu4.isEnabled = true
                bu5.isEnabled = true
                bu6.isEnabled = true
                bu7.isEnabled = true
                bu8.isEnabled = true
                bu9.isEnabled = true
        }

    }

    fun updateTileSymbols(playString:String?, playSymbol:String?){
        // MUST UPDATE LOCAL MYPLAY AND USERPLAY STRINGS BEFORE CALLING THIS FUNCTION

        if(playString == null){
            return
        }
        if(playSymbol == "x"){
            //Player who plays first

            when(playString[playString.length-1]){
                '1'-> { bu1.setImageResource(R.drawable.ic_player_x)
                    bu1.isEnabled = false }
                '2'-> {bu2.setImageResource(R.drawable.ic_player_x)
                    bu2.isEnabled = false}
                '3'-> {bu3.setImageResource(R.drawable.ic_player_x)
                    bu3.isEnabled = false}
                '4'-> {bu4.setImageResource(R.drawable.ic_player_x)
                    bu4.isEnabled = false}
                '5'-> {bu5.setImageResource(R.drawable.ic_player_x)
                    bu5.isEnabled = false}
                '6'-> {bu6.setImageResource(R.drawable.ic_player_x)
                    bu6.isEnabled = false}
                '7'-> {bu7.setImageResource(R.drawable.ic_player_x)
                    bu7.isEnabled = false}
                '8'-> {bu8.setImageResource(R.drawable.ic_player_x)
                    bu8.isEnabled = false}
                '9'-> {bu9.setImageResource(R.drawable.ic_player_x)
                    bu9.isEnabled = false}
            }
        }
        else{
            when(playString[playString.length-1]){
                '1'-> { bu1.setImageResource(R.drawable.ic_player_o)
                    bu1.isEnabled = false }
                '2'-> {bu2.setImageResource(R.drawable.ic_player_o)
                    bu2.isEnabled = false}
                '3'-> {bu3.setImageResource(R.drawable.ic_player_o)
                    bu3.isEnabled = false}
                '4'-> {bu4.setImageResource(R.drawable.ic_player_o)
                    bu4.isEnabled = false}
                '5'-> {bu5.setImageResource(R.drawable.ic_player_o)
                    bu5.isEnabled = false}
                '6'-> {bu6.setImageResource(R.drawable.ic_player_o)
                    bu6.isEnabled = false}
                '7'-> {bu7.setImageResource(R.drawable.ic_player_o)
                    bu7.isEnabled = false}
                '8'-> {bu8.setImageResource(R.drawable.ic_player_o)
                    bu8.isEnabled = false}
                '9'-> {bu9.setImageResource(R.drawable.ic_player_o)
                    bu9.isEnabled = false}
            }
        }
    }

    fun updateSymbols(){
        myRef.child("users").child(SplitEmail(myEmail!!)).child("symbol")
            .addValueEventListener(object : ValueEventListener{

                override fun onDataChange(p0: DataSnapshot) {

                    var datamap = p0.value as HashMap<String,Any>


                    if(datamap != null){
                        var valueSet = datamap.values
                        mySymbol = valueSet.elementAt(0) as String
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })

        myRef.child("users").child(SplitEmail(userEmail!!)).child("symbol")
            .addValueEventListener(object : ValueEventListener{

                override fun onDataChange(p0: DataSnapshot) {
                    var datamap = p0.value as HashMap<String,Any>

                    if(datamap != null){
                        var valueSet = datamap.values
                        userSymbol = valueSet.elementAt(0) as String
                    }

                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }

    fun updatePlayStrings(){
        myRef.child("users").child(SplitEmail(myEmail!!)).child("play").addValueEventListener(object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                try{
                    var datamap = p0.value as HashMap<String,Any>

                    if(datamap != null){
                        var valueSet = datamap.values
                        myPlay = valueSet.elementAt(0) as String   // SUGGESTION: I can add update data tiles and or change turn code here
                    }
                } catch (ex:kotlin.Exception){}
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

        myRef.child("users").child(SplitEmail(userEmail!!)).child("play").addValueEventListener(object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                try{
                    var datamap = p0.value as HashMap<String,Any>

                    if(datamap != null){
                        var valueSet = datamap.values
                        userPlay = valueSet.elementAt(0) as String
                    }

                } catch (ex:kotlin.Exception){}

            }


            override fun onCancelled(p0: DatabaseError) {
            }

        })
    }




    fun updateDatabase(){
        //Update value in online database
        myRef.child("users").child(SplitEmail(myEmail!!)).child("play").removeValue() // Remove value and then add value. Stops multiple value creations
        myRef.child("users").child(SplitEmail(myEmail!!)).child("play").push().setValue(myPlay)
    }


    fun checkWinner(player:String?):Boolean
    {
        if(player == null){
            return false
        }
        if (player.contains('1'))
        {
            if((player.contains('2')&& player.contains('3'))||(player.contains('4')&& player.contains('7'))||(player.contains('5')&& player.contains('9')))
            {
                return true
            }
        }
        if (player.contains('2'))
        {
            if((player.contains('5')&& player.contains('8')))
            {
                return true
            }
        }

        if (player.contains('3'))
        {
            if((player.contains('5')&& player.contains('7'))||(player.contains('6')&& player.contains('9')))
            {
                return true
            }
        }

        if (player.contains('4'))
        {
            if((player.contains('5')&& player.contains('6')))
            {
                return true
            }
        }

        if (player.contains('7'))
        {
            if((player.contains('8')&& player.contains('9')))
            {
                return true
            }
        }

        return false
    }

    fun checkDraw(playString: String?,otherPlayString: String?): Boolean
    {
        if((playString == null) || (otherPlayString == null)){
            return false
        }
        if((playString!!.length + otherPlayString!!.length == 9))
        {
            return true
        }

        return false
    }

    fun disable_all()
    {
        bu1.isEnabled = false
        bu2.isEnabled = false
        bu3.isEnabled = false
        bu4.isEnabled = false
        bu5.isEnabled = false
        bu6.isEnabled = false
        bu7.isEnabled = false
        bu8.isEnabled = false
        bu9.isEnabled = false
    }
}
