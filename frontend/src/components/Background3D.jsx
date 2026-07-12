import { Canvas, useFrame } from '@react-three/fiber'
import { Float, Icosahedron, TorusKnot, MeshDistortMaterial, Stars } from '@react-three/drei'
import { useRef } from 'react'

function GlassBlob() {
  const ref = useRef()
  useFrame((_, delta) => {
    if (ref.current) {
      ref.current.rotation.x += delta * 0.08
      ref.current.rotation.y += delta * 0.1
    }
  })
  return (
    <Float speed={1.1} rotationIntensity={0.6} floatIntensity={1.1}>
      <Icosahedron ref={ref} args={[1, 6]} scale={1.9} position={[3.8, 0.4, -5]}>
        <MeshDistortMaterial
          color="#7c3aed"
          distort={0.42}
          speed={1.6}
          roughness={0.08}
          metalness={0.7}
          emissive="#6d28d9"
          emissiveIntensity={0.15}
          transparent
          opacity={0.55}
        />
      </Icosahedron>
    </Float>
  )
}

function WireKnot() {
  const ref = useRef()
  useFrame((_, delta) => {
    if (ref.current) {
      ref.current.rotation.x += delta * 0.12
      ref.current.rotation.z += delta * 0.06
    }
  })
  return (
    <Float speed={1.4} rotationIntensity={1} floatIntensity={1.4}>
      <TorusKnot ref={ref} args={[1, 0.28, 160, 24, 2, 3]} scale={0.85} position={[-4, -0.5, -1]}>
        <meshStandardMaterial color="#22d3ee" wireframe transparent opacity={0.35} />
      </TorusKnot>
    </Float>
  )
}

function SmallShape() {
  const ref = useRef()
  useFrame((_, delta) => {
    if (ref.current) ref.current.rotation.y += delta * 0.25
  })
  return (
    <Float speed={1.8} rotationIntensity={1.2} floatIntensity={1.6}>
      <Icosahedron ref={ref} args={[1, 1]} scale={0.6} position={[-3, 2.4, -3]}>
        <meshStandardMaterial color="#a78bfa" wireframe transparent opacity={0.4} />
      </Icosahedron>
    </Float>
  )
}

export default function Background3D() {
  return (
    <div
      style={{ position: 'fixed', inset: 0, zIndex: -1, pointerEvents: 'none', opacity: 0.7 }}
      aria-hidden="true"
    >
      <Canvas camera={{ position: [0, 0, 9], fov: 50 }} dpr={[1, 1.8]}>
        <ambientLight intensity={0.35} />
        <pointLight position={[8, 6, 8]} intensity={1.3} color="#22d3ee" />
        <pointLight position={[-8, -4, 4]} intensity={1.1} color="#8b5cf6" />
        <pointLight position={[0, 8, -6]} intensity={0.6} color="#34d399" />
        <Stars radius={70} depth={50} count={1400} factor={2.6} saturation={0} fade speed={0.6} />
        <GlassBlob />
        <WireKnot />
        <SmallShape />
      </Canvas>
    </div>
  )
}
