import java.awt.image.BufferedImage;

import free.lucifer.jtwain.Twain;
import free.lucifer.jtwain.TwainCapability;
import free.lucifer.jtwain.TwainIOMetadata;
import free.lucifer.jtwain.TwainListener;
import free.lucifer.jtwain.TwainScanner;
import free.lucifer.jtwain.TwainSource;
import free.lucifer.jtwain.exceptions.TwainException;
import free.lucifer.jtwain.scan.Source.ColorMode;

public class MySource implements TwainListener {
    private double dpi = 100.0;
    private ColorMode color = ColorMode.COLOR;
    private boolean autoDocumentFeeder = false;
    private boolean systemUI = false;

    private String name;

    private final Object syncObject = new Object();

    private BufferedImage image = null;

    public MySource() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDpi() {
        return dpi;
    }

    public void setDpi(double dpi) {
        this.dpi = dpi;
    }

    public ColorMode getColor() {
        return color;
    }

    public void setColor(ColorMode color) {
        this.color = color;
    }

    public boolean isAutoDocumentFeeder() {
        return autoDocumentFeeder;
    }

    public void setAutoDocumentFeeder(boolean autoDocumentFeeder) {
        this.autoDocumentFeeder = autoDocumentFeeder;
    }

    public boolean isSystemUI() {
        return systemUI;
    }

    public void setSystemUI(boolean systemUI) {
        this.systemUI = systemUI;
    }

    @Override
    public void update(TwainIOMetadata.Type type, TwainIOMetadata metadata) {
//        System.out.println(type + " -> " + metadata.getState() + ": " + metadata.getStateStr());
        if (type == TwainIOMetadata.NEGOTIATE && metadata.getState() == 4) {
            setupSource(metadata.getSource());
        } else if (type == TwainIOMetadata.ACQUIRED && metadata.getState() == 7) {
            image = metadata.getImage();
            metadata.setImage(null);
        } else if (type == TwainIOMetadata.STATECHANGE && metadata.getState() == 3 && metadata.getLastState() == 4) {
            jobDone();
        }
    }

    private void setupSource(TwainSource source) {
        try {
            source.setShowProgressBar(true);
            source.setShowUI(systemUI);

            if (!systemUI) {
//                source.setShowUI(false);
                source.setResolution(dpi);
                TwainCapability pt = source.getCapability(Twain.ICAP_PIXELTYPE);
                switch (color) {
                    case BW:
                        pt.setCurrentValue(0);
                        break;
                    case GRAYSCALE:
                        pt.setCurrentValue(1);
                        break;
                    case COLOR:
                        pt.setCurrentValue(2);
                        break;
                }

                source.setCapability(Twain.CAP_FEEDERENABLED, autoDocumentFeeder ? 1 : 0);
            }
        } catch (TwainException e) {
            e.printStackTrace();
        }
    }


    private void jobDone() {
        // exec.shutdown();
        // try {
        //     while (!exec.awaitTermination(100, TimeUnit.MILLISECONDS)) {
        //     }
        // } catch (InterruptedException ex) {
        // }

        synchronized (syncObject) {
            syncObject.notifyAll();
        }

        // exec = null;
    }

    public BufferedImage scan() {
        //exec = Executors.newFixedThreadPool(1);

        TwainScanner scanner = TwainScanner.getScanner();

        try {
            scanner.select(name);
            scanner.addListener(this);
            scanner.acquire();

            synchronized (syncObject) {
                syncObject.wait();
            }

        } catch (Exception e) {
        }

        return image;
    }

}