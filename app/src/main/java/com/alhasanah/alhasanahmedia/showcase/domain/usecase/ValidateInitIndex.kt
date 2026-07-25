package com.alhasanah.alhasanahmedia.showcase.domain.usecase

import com.alhasanah.alhasanahmedia.showcase.model.ShowcaseMsg


fun validateInitIndex(initIndex: Int, greeting: ShowcaseMsg?): Int {
    return  if (greeting == null) initIndex.coerceAtLeast(1) else initIndex
}
