package org.fossasia.openevent.general.event.topic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_similar_events.moreLikeThis
import kotlinx.android.synthetic.main.fragment_similar_events.progressBar
import kotlinx.android.synthetic.main.fragment_similar_events.similarEventsDivider
import kotlinx.android.synthetic.main.fragment_similar_events.similarEventsRecycler
import kotlinx.android.synthetic.main.fragment_similar_events.view.similarEventsRecycler
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.event.EVENT_ID
import org.fossasia.openevent.general.event.EVENT_TOPIC_ID
import org.fossasia.openevent.general.event.Event
import org.fossasia.openevent.general.event.EventDetailsFragment
import org.fossasia.openevent.general.event.EventsRecyclerAdapter
import org.fossasia.openevent.general.event.FavoriteFabListener
import org.fossasia.openevent.general.event.RecyclerViewClickListener
import org.fossasia.openevent.general.event.SIMILAR_EVENTS
import org.fossasia.openevent.general.utils.extensions.nonNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SimilarEventsFragment : Fragment() {
    private val similarEventsRecyclerAdapter: EventsRecyclerAdapter = EventsRecyclerAdapter()
    private val similarEventsViewModel by viewModel<SimilarEventsViewModel>()
    private lateinit var rootView: View
    private var eventTopicId: Long = -1
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        similarEventsRecyclerAdapter.setEventLayout(SIMILAR_EVENTS)
        val bundle = this.arguments
        var eventId: Long = -1
        if (bundle != null) {
            eventId = bundle.getLong(EVENT_ID, -1)
            eventTopicId = bundle.getLong(EVENT_TOPIC_ID, -1)
        }
        similarEventsViewModel.eventId = eventId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_similar_events, container, false)

        linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rootView.similarEventsRecycler.layoutManager = linearLayoutManager

        rootView.similarEventsRecycler.adapter = similarEventsRecyclerAdapter
        rootView.similarEventsRecycler.isNestedScrollingEnabled = false

        val recyclerViewClickListener = object : RecyclerViewClickListener {
            override fun onClick(eventID: Long) {
                val fragment = EventDetailsFragment()
                val bundle = Bundle()
                bundle.putLong(EVENT_ID, eventID)
                fragment.arguments = bundle
                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.rootLayout, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }

        val favouriteFabClickListener = object : FavoriteFabListener {
            override fun onClick(event: Event, isFavourite: Boolean) {
                val id = similarEventsRecyclerAdapter.getPos(event.id)
                similarEventsViewModel.setFavorite(event.id, !isFavourite)
                event.favorite = !event.favorite
                similarEventsRecyclerAdapter.notifyItemChanged(id)
            }
        }

        similarEventsRecyclerAdapter.setListener(recyclerViewClickListener)
        similarEventsViewModel.similarEvents
            .nonNull()
            .observe(this, Observer {
                similarEventsRecyclerAdapter.addAll(it)
                handleVisibility(it)
                similarEventsRecyclerAdapter.notifyDataSetChanged()
                Timber.d("Fetched similar events of size %s", similarEventsRecyclerAdapter.itemCount)
            })

        similarEventsViewModel.error
            .nonNull()
            .observe(this, Observer {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            })

        similarEventsViewModel.progress
            .nonNull()
            .observe(this, Observer {
                progressBar.isVisible = it
            })

        similarEventsRecyclerAdapter.setFavorite(favouriteFabClickListener)
        similarEventsViewModel.loadSimilarEvents(eventTopicId)

        return rootView
    }

    private fun handleVisibility(similarEvents: List<Event>) {
        similarEventsDivider.isGone = similarEvents.isEmpty()
        moreLikeThis.isGone = similarEvents.isEmpty()
        similarEventsRecycler.isGone = similarEvents.isEmpty()
    }
}
