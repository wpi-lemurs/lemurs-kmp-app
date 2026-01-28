package com.lemurs.lemurs_app.ui.viewmodel

//import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.survey.Demographic
import com.lemurs.lemurs_app.survey.DemographicsSubmission
import com.lemurs.lemurs_app.survey.postDemographics
import androidx.lifecycle.ViewModel

class DemographicsViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    val logger = Logger.withTag("DemographicsViewModel")
    suspend fun submitDemographics(answers : ArrayList<Demographic>) {

        val submission = DemographicsSubmission(answers)
        logger.w("demographics submissions from view model: " + answers)
        postDemographics(submission)
    }
}