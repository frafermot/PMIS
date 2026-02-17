import Quill from 'quill';
import 'quill/dist/quill.snow.css';

window.initQuill = (element) => {

    if (element._initialized) return;
    element._initialized = true;

    const editorContainer = document.createElement('div');
    editorContainer.style.height = "100%";
    element.appendChild(editorContainer);

    const toolbarOptions = [
        ['bold', 'italic', 'underline', 'strike'],
        [{ 'header': [1, 2, 3, false] }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'size': ['small', false, 'large', 'huge'] }],
        [{ 'color': [] }, { 'background': [] }],
        ['image'],
        ['clean']
    ];

    const quill = new Quill(editorContainer, {
        theme: 'snow',
        modules: { toolbar: toolbarOptions }
    });

    element._quill = quill;

    // Cuando cambia el contenido, avisar a Java
    quill.on('text-change', function() {
        const html = quill.root.innerHTML;
        element.$server.updateContent(html);
    });
};

window.setContent = (element, html) => {
    if (element._quill) {
        element._quill.root.innerHTML = html || "";
    }
};
