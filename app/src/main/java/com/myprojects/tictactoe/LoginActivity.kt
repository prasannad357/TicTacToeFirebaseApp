package com.awesome.mystartup

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private var mAuth:FirebaseAuth? = null

    private var database = FirebaseDatabase.getInstance()
    private var myRef = database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

    }

    override fun onStart() {
        super.onStart()

        loadMain()
    }

    fun buLoginEvent (view: View){
        var email = etEmail.text.toString()
        var password = etPassword.text.toString()
        loginToFirebase(email,password)
    }
    fun loginToFirebase(email:String,password:String){

        mAuth!!.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){
                task->
                if(task.isSuccessful){
                    Toast.makeText(this,"Login successful",Toast.LENGTH_LONG).show()
                    var currentUser = mAuth!!.currentUser
                    if(currentUser != null) {
                        var splitEmail = SplitEmail(currentUser.email.toString())
                        myRef.child("users").child(splitEmail).child("request").setValue(currentUser.uid)
                    }

                    loadMain()
                }
                else{
                    Toast.makeText(this,"Login failed",Toast.LENGTH_LONG).show()
                }
            }
    }

    fun loadMain(){
        var currentUser = mAuth!!.currentUser
        if(currentUser != null){
            var intent = Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentUser.email)
            intent.putExtra("uid",currentUser.uid)
            this.startActivity(intent)
        }

    }

    fun SplitEmail(email:String):String{
        var splitEmail = email.split("@")
        return splitEmail[0]
    }
}
