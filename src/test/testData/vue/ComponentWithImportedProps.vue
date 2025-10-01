<template>
  <div>
    <p>{{ name }}</p>
  </div>
</template>

<script>
import { baseProps } from '../js/props';

export default {
  name: 'ComponentWithImportedProps',
  props: {
    ...baseProps,
    extraProp: String
  }
}
</script>
