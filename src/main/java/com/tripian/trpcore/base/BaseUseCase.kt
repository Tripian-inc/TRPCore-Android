package com.tripian.trpcore.base

import android.app.Application
import android.text.TextUtils
import androidx.annotation.CallSuper
import com.google.gson.Gson
import com.tripian.one.util.BaseResponse
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.UseCaseListener
import com.tripian.trpcore.util.dialog.DGContent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseUseCase<Response, Params>(vararg cases: BaseUseCase<*, *>) {

    private val requestTag = this.javaClass.simpleName

    private var useCases: Array<BaseUseCase<*, *>> = arrayOf(*cases)

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var strings: Strings

    @Inject
    lateinit var tripianUserRepository: TripianUserRepository

    @Inject
    lateinit var miscRepository: MiscRepository

    private var success: ((Response) -> Unit)? = null
    private var error: ((ErrorModel) -> Unit)? = null
    private var finally: (() -> Unit)? = null



    var useCaseListener: UseCaseListener? = null
        set(value) {
            for (useCase in useCases) {
                useCase.useCaseListener = value
            }

            field = value
        }

    open fun isRequiredRefreshToken(): Boolean {
        return true
    }

    /**
     * ViewModel kapanması sonrasında içerisindeki RxJava çağrılarının döndürdüğü
     * disposable'ların temizlenmesi için tutulan değişken
     */
    private val compositeDisposable = CompositeDisposable()

    abstract fun on(params: Params? = null)

    /**
     * Servis cagrisinde servisi belirtmek icin kullanilir
     */
    @CallSuper
    fun on(
        params: Params? = null,
        success: ((Response) -> Unit)? = null,
        error: ((ErrorModel) -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        this.success = success
        this.error = error
        this.finally = finally

        clear()

        on(params)
    }

    /**
     * Lifecycle tamamlandığında silinecek disposable'lar tutulur
     *
     * @param disposable disposable
     */
    private fun add(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    /**
     * Memory leak önlenmesi için disposable'lar silinir.
     */
    @CallSuper
    open fun clear() {
        compositeDisposable.clear()
    }

    fun addObservable(task: () -> Observable<Response>) {
        add(
            task()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getResponseListener())
        )
    }

    open fun onSendError(error: ErrorModel) {
        this.error?.invoke(error)
        this.finally?.invoke()
    }

    open fun onSendSuccess(t: Response) {
        this.success?.invoke(t)
        this.finally?.invoke()
    }

    private fun getResponseListener(): DisposableObserver<Response> {
        return object : DisposableObserver<Response>() {
            override fun onComplete() {
            }

            override fun onNext(t: Response & Any) {
                if (t is ResponseModelBase) {
                    if (t.status == 200) {
                        onSendSuccess(t)
                    } else {
                        onSendError(
                            ErrorModel(
                                errorDesc = miscRepository.getLanguageValueForKey(
                                    LanguageConst.COMMON_ERROR
                                )
                            )
                        )
                    }
                } else {
                    onSendSuccess(t)
                }
            }

            @CallSuper
            override fun onError(e: Throwable) {
                when (e) {
                    is ConnectException,
                    is SocketTimeoutException,
                    is SSLHandshakeException -> {
                        onSendError(ErrorModel(miscRepository.getLanguageValueForKey(LanguageConst.NO_NETWORK)))
                    }

                    is HttpException -> {
                        val res = gson.fromJson(
                            (e.response()?.errorBody())?.string(),
                            BaseResponse::class.java
                        )

                        if (e.response()?.raw()?.request?.url.toString().contains("refresh-token")) {
                            tripianUserRepository.logout()
                            refreshTokenError()
                            return
                        }

                        onSendError(
                            ErrorModel(
                                if (!TextUtils.isEmpty(res.message)) {
                                    res.message
                                } else {
                                    miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR)
                                }
                            )
                        )
                    }

                    is Exception -> {
                        onSendError(
                            ErrorModel(
                                if (!TextUtils.isEmpty(e.message)) {
                                    e.message!!
                                } else {
                                    miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR)
                                }
                            )
                        )
                    }

                    is ErrorModel -> {
                        onSendError(e)
                    }

                    else -> {
                        onSendError(
                            ErrorModel(
                                errorDesc = miscRepository.getLanguageValueForKey(
                                    LanguageConst.COMMON_ERROR
                                )
                            )
                        )
                    }
                }
            }

        }
    }

    fun showDialog(dgContent: DGContent) {
        useCaseListener?.showDialog(dgContent)
    }

    fun showSnackBarMessage(message: String) {
        useCaseListener?.showSnackBarMessage(message)
    }

    fun refreshTokenError() {
//        useCaseListener?.refreshTokenError()
    }
}

