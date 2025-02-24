/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tm.alashow.base.util.extensions.Callback
import tm.alashow.base.util.extensions.interpunctize
import tm.alashow.base.util.extensions.orNA
import tm.alashow.common.compose.rememberFlowWithLifecycle
import tm.alashow.datmusic.domain.entities.Album
import tm.alashow.datmusic.ui.detail.MediaDetail
import tm.alashow.ui.components.CoverImage
import tm.alashow.ui.simpleClickable
import tm.alashow.ui.theme.AppTheme

@Composable
fun AlbumDetail() {
    AlbumDetail(viewModel = hiltViewModel())
}

@Composable
private fun AlbumDetail(viewModel: AlbumDetailViewModel) {
    val viewState by rememberFlowWithLifecycle(viewModel.state).collectAsState(initial = AlbumDetailViewState.Empty)

    MediaDetail(
        viewState = viewState,
        titleRes = R.string.albums_detail_title,
        onFailRetry = viewModel::refresh,
        onEmptyRetry = viewModel::refresh,
        mediaDetailContent = AlbumDetailContent(viewState.album ?: Album()),
        headerCoverIcon = rememberVectorPainter(Icons.Default.Album),
        extraHeaderContent = {
            AlbumHeaderSubtitle(viewState, onArtistClick = viewModel::goToArtist)
        },
    )
}

@Composable
private fun AlbumHeaderSubtitle(viewState: AlbumDetailViewState, onArtistClick: Callback) {
    val artist = viewState.album?.artists?.firstOrNull()
    val albumSubtitle = listOfNotNull(stringResource(R.string.albums_detail_title), viewState.album?.displayYear).interpunctize()
    val artistName = artist?.name.orNA()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.specs.paddingSmall)) {
        CoverImage(artist?.photo(), shape = CircleShape, size = 20.dp)
        Text(
            artistName, style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.simpleClickable(onClick = onArtistClick)
        )
    }
    Text(albumSubtitle, style = MaterialTheme.typography.caption)
}
