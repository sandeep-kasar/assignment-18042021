package com.myassignment.ui.main.view

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.myassignment.R
import com.myassignment.utils.PreferenceHelper
import com.myassignment.utils.PreferenceHelper.get
import com.myassignment.utils.PreferenceHelper.set
import kotlinx.android.synthetic.main.fragment_signin.*


class SignInFragment : Fragment(), View.OnClickListener {

    var navController : NavController? = null

    lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        prefs = PreferenceHelper.customPrefs(requireActivity(),"user_info")
        btnSignIn.setOnClickListener(this)
        checkUserLogin()
    }


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnSignIn -> {
                checkValidation()
            }
        }
    }

    private fun checkUserLogin(){
        var userName = prefs["user_name","-1"]
        var password = prefs["password","-1"]
        if (userName != "-1" && password!= "-1"){
            navController?.navigate(R.id.action_mainFragment_to_twoFragment2,
                bundleOf("intent" to "Hello there !!")
            )
        }
    }

    private  fun checkValidation(){

        val userNameIn = edtName.text.toString()
        val passwordIn = edtPass.text.toString()

        if (userNameIn.isNullOrEmpty() || passwordIn.isNullOrEmpty()){
            Toast.makeText(requireActivity(),"Please enter the user name and password",
                Toast.LENGTH_LONG).show()
        }else{
            prefs["user_name"] = userNameIn
            prefs["password"] = passwordIn
            checkUserLogin()
        }
    }
}