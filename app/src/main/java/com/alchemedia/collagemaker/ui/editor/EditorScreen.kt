package com.alchemedia.collagemaker.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alchemedia.collagemaker.R
import com.alchemedia.collagemaker.model.AspectPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val title = (state as? EditorState.Ready)?.project?.name?.ifBlank { null }
        ?: stringResource(R.string.untitled_collage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            EditorState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            EditorState.Missing -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(stringResource(R.string.editor_missing))
            }
            is EditorState.Ready -> Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CollagePreview(
                        project = s.project,
                        photoFile = { viewModel.photoFile(s.project, it) },
                    )
                }
                SectionLabel(R.string.editor_aspect)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AspectPreset.entries) { preset ->
                        FilterChip(
                            selected = preset == s.project.aspect,
                            onClick = { viewModel.setAspect(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
                SectionLabel(R.string.editor_layout)
                LayoutPicker(selectedId = s.project.layoutId, onSelect = viewModel::setLayout)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
    )
}
