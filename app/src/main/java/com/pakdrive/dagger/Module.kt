package com.pakdrive.dagger

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.pakdrive.data.auth.AuthRepo
import com.pakdrive.data.auth.AuthRepoImpl
import com.pakdrive.data.customer.CustomerRepo
import com.pakdrive.data.customer.CustomerRepoImpl
import com.pakdrive.ui.activities.auth.CustomerLoginActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    fun provideAuthRepo(auth: FirebaseAuth,databaseReference: DatabaseReference):AuthRepo{
        return AuthRepoImpl(auth, databaseReference)
    }

    @Provides
    @Singleton
    fun provideCustomerRepo(auth: FirebaseAuth,storageReference: StorageReference,databaseReference: DatabaseReference):CustomerRepo{
        return CustomerRepoImpl(auth, storageReference, databaseReference)
    }


}