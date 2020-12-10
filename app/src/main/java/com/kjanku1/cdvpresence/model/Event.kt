package com.kjanku1.cdvpresence.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Event(
    var cId: Int?=null,
    var title: String?=null,
    var description: String?=null,
    var dtstart: String?=null,
    var dtend: String?=null,
    var event_location: String?=null,
    var url: String?=null
) : Parcelable
