<template>
  <span>nevermind</span>
</template>

<script>
import propsClient from './client_form_props.js'
import propsClientTabs from './client_form_tabs_props.js'
import propsClientAddress from './client_form_address_props.js'
import propsAppointment from './props.js'
import {namedProps} from "./namedProps";


export default {
  name: 'AppointmentCreateModal',

  props: {
    visibility: Boolean,
    loading: Boolean,
    forcedClientEdit: Boolean,
    disableButtons: Boolean,
    cleared: Boolean,
    /**
     * @type {import('vue').PropOptions<{id: number, shortName: string, fullName: string}>}
     */
    doctor: {
      type: Object,
      default: () => ({}),
      required: false,
    },
    date: {
      type: String,
      default: '',
      required: false,
    },

    time: {
      type: String,
      required: false,
      default: '',
    },

    clientId: {
      type: Number,
      default: null,
    },

    clientSummary: {
      type: Object,
      default: () => ({}),
    },

    serviceCard: {
      type: String,
      default: '',
    },

    clientEdited: Boolean,

    ...propsClient,
    ...propsClientTabs,
    ...propsClientAddress,
    ...propsAppointment,
    ...namedProps
  },
}
</script>
