/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.insets.ui.Scaffold
import tm.alashow.base.util.extensions.Callback
import tm.alashow.base.util.extensions.muteUntil
import tm.alashow.base.util.extensions.orNA
import tm.alashow.domain.models.Incomplete
import tm.alashow.navigation.LocalNavigator
import tm.alashow.navigation.Navigator
import tm.alashow.ui.adaptiveColor
import tm.alashow.ui.components.CollapsingTopBar
import tm.alashow.ui.components.fullScreenLoading

@Composable
fun <DetailType> MediaDetail(
    viewState: MediaDetailViewState<DetailType>,
    @StringRes titleRes: Int,
    onTitleClick: Callback = {},
    onFailRetry: Callback,
    onEmptyRetry: Callback,
    mediaDetailContent: MediaDetailContent<DetailType>,
    mediaDetailHeader: MediaDetailHeader = MediaDetailHeader(),
    mediaDetailFail: MediaDetailFail<DetailType> = MediaDetailFail(),
    mediaDetailEmpty: MediaDetailEmpty<DetailType> = MediaDetailEmpty(),
    headerCoverIcon: VectorPainter? = null,
    extraHeaderContent: @Composable ColumnScope.() -> Unit = {},
    navigator: Navigator = LocalNavigator.current,
) {
    val listState = rememberLazyListState()
    val headerOffsetProgress = coverHeaderScrollProgress(listState)
    Scaffold(
        topBar = {
            CollapsingTopBar(
                title = stringResource(titleRes),
                collapsed = headerOffsetProgress.value.muteUntil(0.75f),
                onNavigationClick = {
                    navigator.goBack()
                },
            )
        }
    ) { padding ->
        MediaDetailContent(
            viewState = viewState,
            onFailRetry = onFailRetry,
            onEmptyRetry = onEmptyRetry,
            onTitleClick = onTitleClick,
            padding = padding,
            listState = listState,
            mediaDetailHeader = mediaDetailHeader,
            mediaDetailContent = mediaDetailContent,
            mediaDetailFail = mediaDetailFail,
            mediaDetailEmpty = mediaDetailEmpty,
            headerCoverIcon = headerCoverIcon,
            extraHeaderContent = extraHeaderContent,
        )
    }
}

@Composable
private fun <DetailType, T : MediaDetailViewState<DetailType>> MediaDetailContent(
    viewState: T,
    onFailRetry: Callback,
    onEmptyRetry: Callback,
    onTitleClick: Callback,
    listState: LazyListState,
    mediaDetailContent: MediaDetailContent<DetailType>,
    mediaDetailHeader: MediaDetailHeader,
    mediaDetailFail: MediaDetailFail<DetailType>,
    mediaDetailEmpty: MediaDetailEmpty<DetailType>,
    headerCoverIcon: VectorPainter? = null,
    extraHeaderContent: @Composable ColumnScope.() -> Unit,
    padding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val artwork = viewState.artwork(context)
    val adaptiveColor = adaptiveColor(
        artwork,
        fallback = MaterialTheme.colors.background,
        gradientEndColor = MaterialTheme.colors.background
    )
    val adaptiveBackground = Modifier.background(adaptiveColor.gradient)

    // apply adaptive background to whole list only on light theme
    // because full list gradient doesn't look great on dark
    val isLight = MaterialTheme.colors.isLight
    val listBackgroundMod = if (isLight) adaptiveBackground else Modifier
    val headerBackgroundMod = if (isLight) Modifier else adaptiveBackground

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = padding.calculateTopPadding() + padding.calculateBottomPadding()),
        modifier = listBackgroundMod.fillMaxSize(),
    ) {
        if (viewState.isLoaded) {
            val details = viewState.details()
            val detailsLoading = details is Incomplete

            mediaDetailHeader(
                list = this,
                listState = listState,
                headerBackgroundMod = headerBackgroundMod,
                title = viewState.title.orNA(),
                artwork = artwork,
                onTitleClick = onTitleClick,
                headerCoverIcon = headerCoverIcon,
                extraHeaderContent = extraHeaderContent,
            )

            val isEmpty = mediaDetailContent(
                list = this,
                details = details,
                detailsLoading = detailsLoading
            )

            mediaDetailFail(
                list = this,
                details = details,
                onFailRetry = onFailRetry
            )

            mediaDetailEmpty(
                list = this,
                details = details,
                detailsEmpty = isEmpty,
                onEmptyRetry = onEmptyRetry
            )
        } else {
            fullScreenLoading()
        }
    }
}
