import ClassicEditor from '@ckeditor/ckeditor5-build-classic';

/**
 * Initialise a CKEditor 5 Classic instance on the given element.
 * Fixes:
 *  1. Pending content: if setValue arrives before the async editor is ready,
 *     content is queued and applied once the editor resolves.
 *  2. HTML toggle uses an absolute overlay so the editor's DOM/flex layout
 *     is never disturbed.
 */
window.initCKEditor = (element, initialContent) => {
    if (element._ckInitialized) return;
    element._ckInitialized = true;

    // ── Outer wrapper (full height, flex column) ──────────────────────────────
    const wrapper = document.createElement('div');
    wrapper.style.cssText =
        'display:flex; flex-direction:column; height:100%; position:relative;';

    // "Ver HTML" toggle button
    const toggleBtn = document.createElement('button');
    toggleBtn.textContent = 'Ver HTML';
    toggleBtn.type = 'button';
    toggleBtn.style.cssText =
        'align-self:flex-end; font-size:0.8em; padding:3px 10px; margin-bottom:4px;' +
        'border:1px solid #aaa; border-radius:4px; cursor:pointer; background:#f5f5f5;' +
        'z-index:2; position:relative;';

    // CKEditor container — takes all remaining height
    const editorContainer = document.createElement('div');
    editorContainer.style.cssText = 'flex:1; min-height:0; overflow:auto;';

    // Absolute overlay textarea for raw HTML (hidden by default)
    const htmlArea = document.createElement('textarea');
    htmlArea.style.cssText =
        'display:none; position:absolute; top:32px; left:0; right:0; bottom:0;' +
        'width:100%; box-sizing:border-box; z-index:10;' +
        'font-family:monospace; font-size:0.85em; padding:8px;' +
        'border:1px solid #ccc; background:#fafafa; resize:none;';

    wrapper.appendChild(toggleBtn);
    wrapper.appendChild(editorContainer);
    wrapper.appendChild(htmlArea);
    element.appendChild(wrapper);

    // ── Bootstrap CKEditor ────────────────────────────────────────────────────
    ClassicEditor
        .create(editorContainer, {
            toolbar: {
                items: [
                    'heading', '|',
                    'bold', 'italic', 'underline', '|',
                    'bulletedList', 'numberedList', '|',
                    'insertTable', '|',
                    'link', 'blockQuote', '|',
                    'undo', 'redo'
                ]
            },
            language: 'es'
        })
        .then(editor => {
            element._ckEditor = editor;

            // ── Fix 1: apply pending content queued before editor was ready ──
            const pending = element._pendingContent;
            if (pending !== undefined && pending !== null) {
                editor.setData(pending);
                delete element._pendingContent;
            } else if (initialContent) {
                editor.setData(initialContent);
            }

            if (element._pendingReadOnly !== undefined) {
                window.setCKEditorReadOnly(element, element._pendingReadOnly);
                delete element._pendingReadOnly;
            }

            // Sync changes to the server
            editor.model.document.on('change:data', () => {
                const html = editor.getData();
                element.$server.updateContent(html);
                // Keep textarea sync when it's visible
                if (htmlArea.style.display !== 'none') {
                    htmlArea.value = html;
                }
            });

            // ── Fix 2: HTML toggle uses overlay — editor is never hidden ─────
            let showingHtml = false;
            toggleBtn.addEventListener('click', () => {
                showingHtml = !showingHtml;
                if (showingHtml) {
                    htmlArea.value = editor.getData();
                    htmlArea.style.display = 'block';
                    toggleBtn.textContent = 'Ver editor';
                    // Adjust top to sit just below the button
                    toggleBtn.style.position = 'relative';
                } else {
                    // Push HTML from textarea back into editor
                    try { editor.setData(htmlArea.value); } catch (_) { }
                    htmlArea.style.display = 'none';
                    toggleBtn.textContent = 'Ver HTML';
                }
            });
        })
        .catch(err => {
            console.error('CKEditor init error:', err);
        });
};

window.setCKEditorContent = (element, html) => {
    if (element._ckEditor) {
        // Editor already ready — apply immediately
        element._ckEditor.setData(html || '');
    } else {
        // Editor still initialising — queue for when it resolves (Fix 1)
        element._pendingContent = html || '';
    }
};

window.setCKEditorReadOnly = (element, readOnly) => {
    if (element._ckEditor) {
        if (readOnly) {
            element._ckEditor.enableReadOnlyMode('server');
        } else {
            element._ckEditor.disableReadOnlyMode('server');
        }
        
        const htmlArea = element.querySelector('textarea');
        if (htmlArea) {
            htmlArea.readOnly = readOnly;
        }
        
        const toggleBtn = element.querySelector('button');
        if (toggleBtn) {
            toggleBtn.style.display = readOnly ? 'none' : 'inline-block';
        }
    } else {
        element._pendingReadOnly = readOnly;
    }
};
