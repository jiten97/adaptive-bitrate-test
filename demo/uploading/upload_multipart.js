async function uploadFile() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    const uploadStatus = document.getElementById('uploadStatus');
    const transcodingStatus = document.getElementById('transcodingStatus');

    if (!file) {
        alert('Please select a file.');
        return;
    }

    uploadStatus.innerHTML = 'Initiating upload...';
    const partSize = 10 * 1024 * 1024; // 10MB
    const numParts = Math.ceil(file.size / partSize);
    const fileKey = await initiateMultipartUpload(file.name, file.size, file.type);

    uploadStatus.innerHTML = `File Key: ${fileKey}<br>Uploading...`;

    const promises = [];
    for (let partNum = 1; partNum <= numParts; partNum++) {
        const start = (partNum - 1) * partSize;
        const end = Math.min(start + partSize, file.size);
        const blob = file.slice(start, end);
        const isLast = partNum === numParts;
        promises.push(uploadPart(fileKey, partNum, blob, isLast));
    }

    const eTagDetailArray = await Promise.all(promises);
    const completeMultipartUploadReponse = await completeMultipartUpload(fileKey, eTagDetailArray);
    uploadStatus.innerHTML = `File Key: ${fileKey}<br>Upload complete`;

    // Check transcoding status
    checkTranscodingStatus(completeMultipartUploadReponse.transcodeJobId,completeMultipartUploadReponse.transcodePath);
}

const backendUrlPrefix = "http://localhost:8080/video";

async function initiateMultipartUpload(fileName, fileSize, contentType) {
    const response = await fetch(backendUrlPrefix + '/multipart/v1/initiate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ fileName, fileSize, contentType })
    });
    const data = await response.json();
    return data.fileKey;
}

async function uploadPart(fileKey, partNumber, blob, isLast) {
    const formData = new FormData();
    formData.append('fileKey', fileKey);
    formData.append('partNumber', partNumber);
    formData.append('file', blob);
    formData.append('isLast', isLast);

    const response = await fetch(backendUrlPrefix + '/multipart/v1/upload', {
        method: 'POST',
        body: formData
    });

    const data = await response.json();
    return {
        partNumber: data.partNumber,
        eTag: data.eTag
    };
}

async function completeMultipartUpload(fileKey, eTagDetailArray) {
    const response = await fetch(backendUrlPrefix + '/multipart/v1/complete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ fileKey, eTagDetailArray })
    });

    const data = await response.json();
    console.log('Upload complete:', data);
    return data;
}

async function checkTranscodingStatus(transcodingId,transcodePath) {
    const transcodingStatus = document.getElementById('transcodingStatus');

    // Periodically check the transcoding status
    console.log("Transcoding id received: "+transcodingId)

    const intervalId = setInterval(async () => {
        const response = await fetch(`${backendUrlPrefix}/transcoding/v1/status/${transcodingId}`);
        const data = await response.json();

        transcodingStatus.innerHTML = `Transcoding status: ${data.status}`;

        if (data.status == 'COMPLETE') {
            clearInterval(intervalId);
            playVideo(transcodePath);
        } else if (data.status == 'FAILED') {
            clearInterval(intervalId);
            transcodingStatus.innerHTML = 'Transcoding failed.';
        }
    }, 5000); // Check every 5 seconds
}