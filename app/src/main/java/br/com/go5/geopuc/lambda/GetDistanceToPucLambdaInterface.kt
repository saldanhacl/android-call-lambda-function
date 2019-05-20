package br.com.go5.geopuc.lambda

import br.com.go5.geopuc.model.RequestClass
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction

interface GetDistanceToPucLambdaInterface {

    @LambdaFunction
    fun getDistanceToPuc(request: RequestClass): String

}