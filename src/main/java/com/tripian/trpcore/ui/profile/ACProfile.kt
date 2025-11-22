package com.tripian.trpcore.ui.profile

import android.content.Intent
import android.net.Uri
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcProfileBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.getLanguage
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 6.06.2021.
 */
class ACProfile : BaseActivity<AcProfileBinding, ACProfileVM>() {

    override fun getViewBinding(): AcProfileBinding {
        return AcProfileBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }

        binding.rlCompanion.setOnClickListener { viewModel.onClickedCompanion() }
        binding.rlPersonalInformation.setOnClickListener { viewModel.onClickedPersonal() }
        binding.rlTermsOfUse.setOnClickListener {
            val defaultBrowser = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.tripian.com/terms-conditions.html")
            )
            startActivity(defaultBrowser)
        }
        binding.rlPrivacyPolicy.setOnClickListener {
            val defaultBrowser =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tripian.com/privacy-policy.html"))
            startActivity(defaultBrowser)
        }
        binding.rlAboutUs.setOnClickListener {
            val defaultBrowser =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tripian.com/about.html"))
            startActivity(defaultBrowser)
        }
        binding.rlLanguage.setOnClickListener {
            viewModel.onClickedLanguage()
        }
        binding.btnLogout.setOnClickListener { viewModel.logout() }
        binding.tvUser.text = getLanguageForKey(LanguageConst.USER)
        binding.tvProfile.text = getLanguageForKey(LanguageConst.PROFILE)
        binding.tvCompanion.text = getLanguageForKey(LanguageConst.TRAVEL_COMPANIONS)
        binding.tvSupport.text = getLanguageForKey(LanguageConst.SUPPORT)
        binding.tvTOE.text = getLanguageForKey(LanguageConst.TOE)
        binding.tvPP.text = getLanguageForKey(LanguageConst.PP)
        binding.tvAbout.text = getLanguageForKey(LanguageConst.ABOUT_TRIPIAN)
        binding.tvTradeMark.text = getLanguageForKey(LanguageConst.TRADEMARK)
        binding.btnLogout.text = getLanguageForKey(LanguageConst.LOG_OUT)
        val langText = getLanguageForKey(LanguageConst.LANGUAGE) + " - " + getLanguage().uppercase()
        binding.tvLanguage.text = langText
    }

    override fun setReceivers() {
        observe(viewModel.onSetUserNameListener) {
            binding.tvName.text = it
        }
    }
}