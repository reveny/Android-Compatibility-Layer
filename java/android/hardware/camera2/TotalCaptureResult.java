package android.hardware.camera2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TotalCaptureResult extends CaptureResult
{
    private final List<CaptureResult> mPartialResults;
    private final Map<String, TotalCaptureResult> mPhysicalResults;

    TotalCaptureResult(String cameraId, Map<Key<?>, Object> values, CaptureRequest request, int sequenceId, long frameNumber,
            List<CaptureResult> partialResults, Map<String, TotalCaptureResult> physicalResults)
    {
        super(cameraId, values, request, sequenceId, frameNumber);
        mPartialResults = partialResults == null ? Collections.<CaptureResult>emptyList() : Collections.unmodifiableList(partialResults);
        mPhysicalResults = physicalResults == null ? new HashMap<>() : physicalResults;
    }

    public List<CaptureResult> getPartialResults()
    {
        return mPartialResults;
    }

    public Map<String, CaptureResult> getPhysicalCameraResults()
    {
        return Collections.unmodifiableMap(new HashMap<String, CaptureResult>(mPhysicalResults));
    }

    public Map<String, TotalCaptureResult> getPhysicalCameraTotalResults()
    {
        return Collections.unmodifiableMap(mPhysicalResults);
    }
}
