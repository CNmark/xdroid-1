package android.ext.eventbus;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.ext.core.Objects;
import android.ext.customservice.CustomService;
import android.ext.customservice.CustomServiceResolver;
import android.ext.inflater.Inflatable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import static android.ext.eventbus.BuildConfig.SNAPSHOT;
import static android.ext.eventbus.EventBus.getEventName;

/**
 * @author Oleksii Kropachov (o.kropachov@shamanland.com)
 */
public class EventDelivery extends DefaultEventDispatcher implements Inflatable {
    private static final String LOG_TAG = EventDelivery.class.getSimpleName();

    private final Context mContext;
    private final FragmentManager mManager;
    private EventDeliveryOptions mOptions;

    public EventDeliveryOptions getOptions() {
        return mOptions;
    }

    public void setOptions(EventDeliveryOptions options) {
        mOptions = options;
    }

    public EventDelivery(Context context) {
        mContext = Objects.notNull(context);
        mManager = Objects.notNull(CustomService.get(context, FragmentManager.class));
    }

    @Override
    protected boolean performOnNewEvent(int eventId, Bundle event) {
        Fragment fragment = mManager.findFragmentByTag(mOptions.tag);
        if (fragment != null) {
            if (!fragment.isVisible()) {
                mOptions.performTransaction(mManager, fragment);
            }

            if (fragment instanceof CustomServiceResolver) {
                Object dispatcher = ((CustomServiceResolver) fragment).getCustomService(EventDispatcher.class.getSimpleName());
                if (dispatcher instanceof EventDispatcher) {
                    if (SNAPSHOT) {
                        Log.v(LOG_TAG, "performOnNewEvent: " + getEventName(eventId) + " delivering to " + fragment + debugThis());
                    }

                    return ((EventDispatcher) dispatcher).onNewEvent(eventId, event);
                } else {
                    if (SNAPSHOT) {
                        Log.i(LOG_TAG, "performOnNewEvent: " + getEventName(eventId) + " no dispatcher found for " + fragment + debugThis());
                    }

                    // NOTE assume that event handled even if no custom dispatcher
                    return true;
                }
            } else {
                if (SNAPSHOT) {
                    Log.w(LOG_TAG, "performOnNewEvent: " + getEventName(eventId) + " failed to deliver for " + fragment + debugThis());
                }

                return false;
            }
        } else {
            if (SNAPSHOT) {
                Log.v(LOG_TAG, "performOnNewEvent: " + getEventName(eventId) + " instantiating of " + mOptions.fragment + debugThis());
            }

            mOptions.performTransaction(mManager, Fragment.instantiate(mContext, mOptions.fragment, EventBus.prepare(eventId, event)));
            return true;
        }
    }

    @Override
    public void inflate(Context context, XmlPullParser parser, AttributeSet attrs) {
        setOptions(new EventDeliveryOptions(context, attrs));
    }
}
