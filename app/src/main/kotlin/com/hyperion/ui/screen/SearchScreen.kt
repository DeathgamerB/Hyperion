package com.hyperion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.hyperion.R
import com.hyperion.domain.model.DomainSearch
import com.hyperion.domain.model.DomainVideoPartial
import com.hyperion.ui.component.ChannelCard
import com.hyperion.ui.component.PlaylistCard
import com.hyperion.ui.component.VideoCard
import com.hyperion.ui.navigation.AppDestination
import com.hyperion.ui.viewmodel.SearchViewModel
import com.xinto.taxi.BackstackNavigator
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = getViewModel(),
    navigator: BackstackNavigator<AppDestination>
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            SmallTopAppBar(
                title = {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { showResults = !it.isFocused },
                        value = viewModel.search,
                        onValueChange = { viewModel.getSuggestions(it) },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search)) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (viewModel.search.isNotBlank()) {
                                        viewModel.getResults()
                                        focusManager.clearFocus()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (viewModel.search.isNotBlank()) {
                                    viewModel.getResults()
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator::pop) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val results = viewModel.results.collectAsLazyPagingItems()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showResults) {
                items(results) { result ->
                    if (result == null) return@items

                    when (result) {
                        is DomainSearch.Result.Video -> {
                            VideoCard(
                                video = DomainVideoPartial(
                                    id = result.id,
                                    title = result.title,
                                    subtitle = result.subtitle,
                                    author = result.author,
                                    timestamp = result.timestamp
                                ),
                                onClick = { navigator.push(AppDestination.Player(result.id)) },
                                onClickChannel = { navigator.push(AppDestination.Channel(result.author!!.id)) }
                            )
                        }
                        is DomainSearch.Result.Channel -> {
                            ChannelCard(
                                channel = result,
                                onClick = { navigator.push(AppDestination.Channel(result.id)) },
                                onSubscribe = { }
                            )
                        }
                        is DomainSearch.Result.Playlist -> {
                            PlaylistCard(
                                playlist = result,
                                onClick = { /* TODO */ }
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        results.loadState.apply {
                            when {
                                refresh is LoadState.Loading -> {
                                    CircularProgressIndicator()
                                }
                                append is LoadState.Loading -> {
                                    CircularProgressIndicator()
                                }
                                append is LoadState.Error -> {
                                    (append as LoadState.Error).error.message?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(viewModel.suggestions) { suggestion ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.search(suggestion)
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 8.dp),
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}